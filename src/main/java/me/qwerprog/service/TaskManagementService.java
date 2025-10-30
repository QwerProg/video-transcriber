package me.qwerprog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.qwerprog.dto.TaskInfo;
import me.qwerprog.integration.WhisperClient; // For keys
import me.qwerprog.integration.YtDlpClient;   // For keys
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import me.qwerprog.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.qwerprog.VideoTranscriberApplication.TEMP_DIR;

/**
 * 管理视频处理任务的状态和生命周期.
 * 包含任务的创建、状态更新、持久化和异步执行.
 */
@Service
public class TaskManagementService {

    private static final Logger log = LoggerFactory.getLogger(TaskManagementService.class);

    // 线程安全的 Map 用于存储任务信息
    private final ConcurrentMap<String, TaskInfo> tasks = new ConcurrentHashMap<>();
    // 线程安全的 Set 用于跟踪正在处理的 URL
    private final ConcurrentMap<String, String> processingUrls = new ConcurrentHashMap<>(); // URL -> TaskId

    private final VideoProcessingService videoProcessingService;
    private final TranscriptionService transcriptionService;
    private final SummarizationService summarizationService;
    private final TranslationService translationService;
    private final SseService sseService;
    private final Executor taskExecutor; // 异步任务执行器
    private final ObjectMapper objectMapper; // 用于 JSON 持久化

    @Value("${task.status.file:temp/tasks.json}") // 从配置读取持久化文件路径
    private String tasksFilePath;
    private Path tasksFile;

    // 文件名清理正则 (保留字母数字、下划线、连字符、点)
    private static final Pattern FILENAME_SANITIZE_PATTERN = Pattern.compile("[^\\w\\-.]+");
    private static final int MAX_FILENAME_LENGTH = 100; // 文件名最大长度


    @Autowired
    public TaskManagementService(VideoProcessingService videoProcessingService,
                                 TranscriptionService transcriptionService,
                                 SummarizationService summarizationService,
                                 TranslationService translationService,
                                 SseService sseService,
                                 @Qualifier("taskExecutor") Executor taskExecutor) {
        this.videoProcessingService = videoProcessingService;
        this.transcriptionService = transcriptionService;
        this.summarizationService = summarizationService;
        this.translationService = translationService;
        this.sseService = sseService;
        this.taskExecutor = taskExecutor;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT); // 格式化输出 JSON
    }

    /**
     * 初始化服务，加载持久化的任务状态.
     */
    @PostConstruct
    public void initialize() {
        this.tasksFile = Paths.get(tasksFilePath);
        loadTasks();
        log.info("TaskManagementService initialized. Loaded {} tasks.", tasks.size());
    }

    /**
     * 服务销毁前，持久化当前任务状态.
     */
    @PreDestroy
    public void shutdown() {
        saveTasks();
        log.info("TaskManagementService shutting down. Saved {} tasks.", tasks.size());
    }

    /**
     * 创建一个新任务并异步启动处理流程.
     * @param url 视频 URL
     * @param summaryLanguage 摘要语言
     * @return 创建的任务 ID
     * @throws IllegalStateException 如果 URL 已在处理中
     */
    public String createAndStartTask(String url, String summaryLanguage) {
        // 检查 URL 是否已在处理
        String existingTaskId = processingUrls.computeIfAbsent(url, k -> {
            String newTaskId = UUID.randomUUID().toString();
            TaskInfo newTask = new TaskInfo();
            newTask.setTaskId(newTaskId);
            newTask.setUrl(url);
            newTask.setSummaryLanguage(summaryLanguage);
            newTask.updateStatus(TaskInfo.TaskStatus.PROCESSING, "任务创建，准备处理...");
            tasks.put(newTaskId, newTask);
            log.info("Creating new task {} for URL: {}", newTaskId, url);
            saveTasks(); // 创建后立即保存一次
            // 异步启动处理流程
            Future<?> future = CompletableFuture.runAsync(() -> processVideoTask(newTaskId, url, summaryLanguage), taskExecutor)
                    .exceptionally(ex -> {
                        log.error("Task {} failed exceptionally: {}", newTaskId, ex.getMessage(), ex);
                        handleTaskFailure(newTaskId, "Unexpected task execution error: " + ex.getMessage());
                        return null; // CompletableFuture 需要返回值
                    });
            newTask.setTaskFuture(future); // 保存 Future 用于取消
            return newTaskId; // 返回新任务 ID
        });

        // 如果 computeIfAbsent 返回的是已存在的 TaskId，说明 URL 正在处理
        if (!existingTaskId.equals(tasks.get(existingTaskId).getTaskId())) {
            // 这通常不应该发生，除非 computeIfAbsent 的逻辑有问题
            log.warn("URL {} is already being processed under task ID {}.", url, existingTaskId);
            // 返回现有任务 ID，前端应处理 "正在处理中" 的消息
        }

        // 无论如何都广播一次初始状态
        TaskInfo currentTask = tasks.get(existingTaskId);
        if (currentTask != null) {
            sseService.sendTaskUpdate(existingTaskId, currentTask);
        }

        return existingTaskId;
    }

    /**
     * 检查指定的 URL 当前是否正在处理.
     * @param url 视频 URL
     * @return 如果正在处理则返回 true
     */
    public boolean isUrlCurrentlyProcessing(String url) {
        return processingUrls.containsKey(url);
    }


    /**
     * 获取任务信息.
     * @param taskId 任务 ID
     * @return TaskInfo 对象
     * @throws IllegalArgumentException 如果任务不存在
     */
    public TaskInfo getTask(String taskId) {
        TaskInfo taskInfo = tasks.get(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }
        return taskInfo;
    }

    /**
     * 异步处理视频任务的核心逻辑.
     * @param taskId 任务 ID
     * @param url 视频 URL
     * @param summaryLanguage 摘要语言
     */
    // @Async("taskExecutor") // 或者在这里使用 @Async
    public void processVideoTask(String taskId, String url, String summaryLanguage) {
        TaskInfo task = getTask(taskId); // 获取任务对象
        Path audioPath = null; // 用于存储临时音频文件路径

        try {
            // 1. 更新状态：解析和下载视频
            updateTaskProgress(taskId, 10, "正在解析视频信息...");
            Map<String, String> videoInfo = videoProcessingService.getVideoInfo(url);
            String videoTitle = videoInfo.getOrDefault(YtDlpClient.TITLE_KEY, "untitled");
            task.setVideoTitle(videoTitle); // 保存标题
            log.info("Task {}: Video title - '{}'", taskId, videoTitle);


            updateTaskProgress(taskId, 15, "正在下载视频并转换为音频...");
            Map<String, Path> downloadResult = videoProcessingService.downloadAndConvertToAudio(url, TEMP_DIR);
            audioPath = downloadResult.get(YtDlpClient.AUDIO_PATH_KEY);
            task.setAudioFilePath(audioPath != null ? audioPath.toString() : null); // 保存临时音频路径

            // 2. 更新状态：转录音频
            updateTaskProgress(taskId, 35, "音频处理完成，开始转录...");
            Map<String, String> transcriptionResult = transcriptionService.transcribeAudio(audioPath, null); // 自动检测语言
            String rawScript = transcriptionResult.getOrDefault(WhisperClient.TRANSCRIPT_KEY, "");
            String detectedLanguage = transcriptionResult.getOrDefault(WhisperClient.DETECTED_LANGUAGE_KEY, "en");
            task.setDetectedLanguage(detectedLanguage); // 保存检测到的语言
            log.info("Task {}: Transcription complete. Detected language: {}", taskId, detectedLanguage);

            // 保存原始转录稿
            String safeTitle = sanitizeFilename(videoTitle);
            String rawScriptName = String.format("raw_%s_%s.md", safeTitle, taskId.substring(0, 6));
            Path rawScriptPath = TEMP_DIR.resolve(rawScriptName);
            FileUtils.saveStringToFile(rawScript + "\n\nsource: " + url + "\n", rawScriptPath);
            task.setRawScriptPath(rawScriptPath.toString());
            log.info("Task {}: Saved raw transcript to {}", taskId, rawScriptPath);


            // 3. 更新状态：优化转录文本
            updateTaskProgress(taskId, 55, "转录完成，正在优化文本...");
            String optimizedScript = summarizationService.optimizeTranscript(rawScript);
            // 添加标题和来源
            String finalScriptContent = String.format("# %s\n\n%s\n\nsource: %s\n", videoTitle, optimizedScript, url);
            String scriptName = String.format("transcript_%s_%s.md", safeTitle, taskId.substring(0, 6));
            Path scriptPath = TEMP_DIR.resolve(scriptName);
            FileUtils.saveStringToFile(finalScriptContent, scriptPath);
            task.setScriptPath(scriptPath.toString());
            log.info("Task {}: Saved optimized transcript to {}", taskId, scriptPath);


            // 4. 更新状态：翻译（如果需要）
            Path translationPath = null;
            String finalTranslationContent = null;
            if (translationService.shouldTranslate(detectedLanguage, summaryLanguage)) {
                updateTaskProgress(taskId, 70, "正在翻译文本...");
                String translatedText = translationService.translateText(optimizedScript, summaryLanguage, detectedLanguage); // 翻译优化后的文本
                // 添加标题和来源
                finalTranslationContent = String.format("# %s\n\n%s\n\nsource: %s\n", videoTitle, translatedText, url);
                String translationName = String.format("translation_%s_%s.md", safeTitle, taskId.substring(0, 6));
                translationPath = TEMP_DIR.resolve(translationName);
                FileUtils.saveStringToFile(finalTranslationContent, translationPath);
                task.setTranslationPath(translationPath.toString());
                log.info("Task {}: Saved translation to {}", taskId, translationPath);
            } else {
                log.info("Task {}: Translation not required ({} -> {})", taskId, detectedLanguage, summaryLanguage);
            }


            // 5. 更新状态：生成摘要
            updateTaskProgress(taskId, 80, "正在生成摘要...");
            // 使用优化后的脚本生成摘要
            String summary = summarizationService.summarize(optimizedScript, summaryLanguage, videoTitle);
            // 添加来源信息到摘要末尾
            String finalSummaryContent = summary + "\n\nsource: " + url + "\n";
            String summaryName = String.format("summary_%s_%s.md", safeTitle, taskId.substring(0, 6));
            Path summaryPath = TEMP_DIR.resolve(summaryName);
            FileUtils.saveStringToFile(finalSummaryContent, summaryPath);
            task.setSummaryPath(summaryPath.toString());
            log.info("Task {}: Saved summary to {}", taskId, summaryPath);

            // 6. 标记任务完成
            task.setResultPaths(scriptPath, summaryPath, translationPath, rawScriptPath);
            task.setVideoDetails(videoTitle, detectedLanguage, summaryLanguage);
            task.complete("处理完成！");
            log.info("✅ Task {} completed successfully.", taskId);
            sseService.sendTaskUpdate(taskId, task); // 发送最终状态

        } catch (CancellationException e) {
            log.warn("Task {} was cancelled.", taskId);
            handleTaskFailure(taskId, "Task was cancelled by user.");
        } catch (Exception e) {
            log.error("❌ Task {} failed: {}", taskId, e.getMessage(), e);
            handleTaskFailure(taskId, "Processing failed: " + e.getMessage());

        } finally {
            // 清理：从正在处理的 URL 集合中移除
            processingUrls.remove(url);
            // 清理：可选地删除临时音频文件
            cleanupTempFiles(audioPath);
            // 确保保存最终状态
            saveTasks();
            // 关闭 SSE 连接器
            sseService.completeEmitter(taskId);
        }
    }

    /**
     * 更新任务进度并推送 SSE 消息.
     * @param taskId 任务 ID
     * @param progress 进度百分比
     * @param message 状态消息
     */
    private void updateTaskProgress(String taskId, int progress, String message) {
        TaskInfo task = getTask(taskId);
        if (task.getStatus() == TaskInfo.TaskStatus.PROCESSING) { // 只有在处理中才更新进度
            task.updateProgress(progress, message);
            log.debug("Task {} progress: {}% - {}", taskId, progress, message);
            sseService.sendTaskUpdate(taskId, task);
            saveTasks(); // 频繁保存进度
        } else {
            log.warn("Attempted to update progress for non-processing task {}: status={}", taskId, task.getStatus());
        }
    }

    /**
     * 处理任务失败的通用逻辑.
     * @param taskId 任务 ID
     * @param errorMessage 错误消息
     */
    private void handleTaskFailure(String taskId, String errorMessage) {
        try {
            TaskInfo task = getTask(taskId);
            task.fail(errorMessage, "处理失败: " + errorMessage.substring(0, Math.min(errorMessage.length(), 100)) + "..."); // 截断错误消息
            sseService.sendTaskUpdate(taskId, task); // 发送错误状态
            saveTasks();
            // 确保 URL 从 processingUrls 中移除
            if (task.getUrl() != null) {
                processingUrls.remove(task.getUrl());
            }
            // 关闭 SSE 连接器
            sseService.completeEmitter(taskId);
        } catch (IllegalArgumentException e) {
            log.error("Cannot handle failure for non-existent task {}", taskId);
        } catch (Exception e) {
            log.error("Error during task failure handling for task {}: {}", taskId, e.getMessage(), e);
        }
    }


    /**
     * 取消并删除任务.
     * @param taskId 任务 ID
     */
    public void cancelAndDeleteTask(String taskId) {
        TaskInfo task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // 尝试取消正在运行的异步任务
        Future<?> future = task.getTaskFuture();
        if (future != null && !future.isDone()) {
            log.info("Attempting to cancel running task: {}", taskId);
            future.cancel(true); // true 表示尝试中断线程
            // 更新状态为错误或取消
            task.fail("Task cancelled by user.", "任务已取消");
            sseService.sendTaskUpdate(taskId, task);
        }

        // 从内存中移除
        tasks.remove(taskId);
        // 从正在处理的 URL 中移除
        if (task.getUrl() != null) {
            processingUrls.remove(task.getUrl());
        }
        // 关闭相关的 SSE 连接
        sseService.completeEmitter(taskId);
        // 删除相关的临时文件 (可选，根据策略决定)
        // cleanupTaskFiles(task);

        log.info("Task {} cancelled and removed.", taskId);
        // 保存移除后的状态
        saveTasks();
    }

    /**
     * 清理任务相关的临时文件 (示例).
     * @param task 任务信息
     */
    private void cleanupTaskFiles(TaskInfo task) {
        log.debug("Cleaning up files for task {}", task.getTaskId());
        deleteFileIfExists(task.getAudioFilePath());
        deleteFileIfExists(task.getScriptPath());
        deleteFileIfExists(task.getSummaryPath());
        deleteFileIfExists(task.getTranslationPath());
        deleteFileIfExists(task.getRawScriptPath());
    }

    private void deleteFileIfExists(String filePath) {
        if (filePath != null) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.debug("Deleted temporary file: {}", path);
                }
            } catch (Exception e) {
                log.warn("Failed to delete temporary file {}: {}", filePath, e.getMessage());
            }
        }
    }

    /**
     * 清理临时文件 (简化版，仅删除音频).
     * @param audioPath 音频文件路径
     */
    private void cleanupTempFiles(Path audioPath) {
        if (audioPath != null) {
            try {
                if (Files.exists(audioPath)) {
                    Files.delete(audioPath);
                    log.info("Cleaned up temporary audio file: {}", audioPath);
                }
            } catch (IOException e) {
                log.warn("Failed to clean up temporary audio file {}: {}", audioPath, e.getMessage());
            }
        }
    }


    /**
     * 加载任务状态从文件.
     */
    private void loadTasks() {
        if (Files.exists(tasksFile)) {
            try {
                byte[] jsonData = Files.readAllBytes(tasksFile);
                ConcurrentMap<String, TaskInfo> loadedTasks = objectMapper.readValue(jsonData,
                        new TypeReference<ConcurrentHashMap<String, TaskInfo>>() {});

                tasks.clear(); // 清空当前内存中的任务
                // 重新填充 tasks 和 processingUrls
                loadedTasks.forEach((id, task) -> {
                    tasks.put(id, task);
                    // 如果任务仍在处理中，重新添加到 processingUrls
                    // 注意：重启后 Future 会丢失，任务不会自动继续，需要重新触发或标记为失败
                    if (task.getStatus() == TaskInfo.TaskStatus.PROCESSING) {
                        log.warn("Task {} was in PROCESSING state upon loading. Resetting to ERROR as process was interrupted.", id);
                        task.fail("Application restarted during processing.", "处理中断");
                        // 不再添加到 processingUrls，因为它实际上没有在处理
                    }
                    // 清理已完成或错误任务的 Future
                    task.setTaskFuture(null);
                });


                log.info("Successfully loaded {} tasks from {}", tasks.size(), tasksFile.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to load tasks from {}: {}", tasksFile.toAbsolutePath(), e.getMessage(), e);
                // 加载失败，可以考虑备份旧文件并开始一个空的任务列表
            }
        } else {
            log.info("Tasks status file not found at {}. Starting with an empty task list.", tasksFile.toAbsolutePath());
        }
    }

    /**
     * 保存当前任务状态到文件 (原子写入).
     */
    private synchronized void saveTasks() { // 同步确保写入一致性
        Path tempSaveFile = tasksFile.getParent().resolve(tasksFile.getFileName() + ".tmp");
        try {
            // 先写入临时文件
            Files.write(tempSaveFile, objectMapper.writeValueAsBytes(tasks));
            // 原子移动替换旧文件
            Files.move(tempSaveFile, tasksFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.debug("Successfully saved {} tasks to {}", tasks.size(), tasksFile.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save tasks to {}: {}", tasksFile.toAbsolutePath(), e.getMessage(), e);
            // 尝试删除临时文件
            try { Files.deleteIfExists(tempSaveFile); } catch (IOException ignored) {}
        }
    }

    /**
     * 清理文件名，移除不安全字符并截断.
     * @param originalName 原始文件名 (通常来自视频标题)
     * @return 清理后的安全文件名
     */
    private String sanitizeFilename(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "untitled";
        }
        // 移除或替换不安全字符
        String sanitized = FILENAME_SANITIZE_PATTERN.matcher(originalName).replaceAll("_");
        // 替换连续的下划线为一个
        sanitized = sanitized.replaceAll("_+", "_");
        // 移除开头和结尾的下划线
        sanitized = sanitized.replaceAll("^_|_$", "");

        // 截断文件名以防止过长
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH);
            // 确保截断后不以 '_' 结尾
            sanitized = sanitized.replaceAll("_$", "");
        }
        // 如果清理后为空，返回默认值
        return sanitized.isEmpty() ? "untitled" : sanitized;
    }

}
