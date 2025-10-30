package me.qwerprog.service;

import me.qwerprog.dto.TaskInfo;
import me.qwerprog.dto.TaskStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 处理 Server-Sent Events (SSE) 推送的服务.
 */
@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    // 存储每个任务 ID 对应的 SseEmitter 列表 (一个任务可能有多个监听者)
    // 使用 ConcurrentHashMap 和 CopyOnWriteArrayList 来确保线程安全
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // SSE 连接超时时间 (例如 30 分钟)

    /**
     * 为指定的任务 ID 创建一个新的 SseEmitter.
     * @param taskId 任务 ID
     * @return SseEmitter 实例
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT); // 设置超时时间

        // 使用 computeIfAbsent 确保线程安全地初始化列表
        CopyOnWriteArrayList<SseEmitter> taskEmitters = emitters.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>());
        taskEmitters.add(emitter);
        log.info("Emitter created and added for task: {}. Total emitters for task: {}", taskId, taskEmitters.size());


        // 设置 Emitter 完成时的回调
        emitter.onCompletion(() -> {
            log.info("Emitter completed for task: {}", taskId);
            removeEmitter(taskId, emitter);
        });

        // 设置 Emitter 超时的回调
        emitter.onTimeout(() -> {
            log.warn("Emitter timed out for task: {}", taskId);
            emitter.complete(); // 显式完成
            // removeEmitter(taskId, emitter); // onCompletion 会处理移除
        });

        // 设置 Emitter 错误时的回调
        emitter.onError(e -> {
            log.error("Emitter error for task: {}: {}", taskId, e.getMessage());
            // 尝试完成 Emitter，但不保证成功
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {}
            removeEmitter(taskId, emitter);
        });

        // 立刻发送一次当前任务状态 (可选，取决于 TaskManagementService 是否已发送初始状态)
        // TaskInfo currentTask = taskManagementService.getTask(taskId); // 需要注入 TaskManagementService
        // sendTaskUpdate(taskId, currentTask);

        return emitter;
    }

    /**
     * 向指定任务的所有监听者发送任务更新.
     * @param taskId 任务 ID
     * @param taskInfo 任务信息
     */
    public void sendTaskUpdate(String taskId, TaskInfo taskInfo) {
        CopyOnWriteArrayList<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters == null || taskEmitters.isEmpty()) {
            log.debug("No active emitters found for task: {}", taskId);
            return;
        }

        TaskStatusResponse response = new TaskStatusResponse(taskInfo);
        String jsonData = response.toJsonString(); // 获取 JSON 字符串

        // 迭代并移除无效的 emitter
        for (SseEmitter emitter : taskEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis())) // 事件 ID
                        .name("task_update") // 事件名称
                        .data(jsonData, MediaType.APPLICATION_JSON)); // 发送 JSON 字符串
                log.debug("Sent update for task {} to emitter: {}", taskId, emitter);
            } catch (IOException e) {
                log.warn("Failed to send update to emitter for task {}: {}. Removing emitter.", taskId, e.getMessage());
                removeEmitter(taskId, emitter); // 发送失败时移除
            } catch (IllegalStateException e) {
                log.warn("Emitter for task {} is already completed or has error: {}. Removing emitter.", taskId, e.getMessage());
                removeEmitter(taskId, emitter); // Emitter 已失效，移除
            }
        }
        log.debug("Finished sending updates for task {}. Active emitters remaining: {}", taskId, taskEmitters.size());
    }

    /**
     * 向指定任务的所有监听者发送心跳消息.
     * @param taskId 任务 ID
     */
    public void sendHeartbeat(String taskId) {
        CopyOnWriteArrayList<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters != null && !taskEmitters.isEmpty()) {
            String heartbeatData = TaskStatusResponse.heartbeatJson();
            for (SseEmitter emitter : taskEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(heartbeatData, MediaType.APPLICATION_JSON));
                    log.trace("Sent heartbeat for task {} to emitter: {}", taskId, emitter);
                } catch (IOException | IllegalStateException e) {
                    log.warn("Failed to send heartbeat to emitter for task {}: {}. Removing emitter.", taskId, e.getMessage());
                    removeEmitter(taskId, emitter);
                }
            }
        }
    }

    /**
     * 完成指定任务的所有 SseEmitter.
     * @param taskId 任务 ID
     */
    public void completeEmitter(String taskId) {
        CopyOnWriteArrayList<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters != null) {
            log.info("Completing all emitters for task: {}", taskId);
            // 遍历并完成
            for (SseEmitter emitter : taskEmitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("Error while completing emitter for task {}: {}", taskId, e.getMessage());
                }
            }
            // completion 回调会自动处理移除，这里可以不显式移除
            // emitters.remove(taskId);
        }
    }


    /**
     * 从列表中安全地移除一个 SseEmitter.
     * @param taskId 任务 ID
     * @param emitter 要移除的 Emitter
     */
    private void removeEmitter(String taskId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters != null) {
            boolean removed = taskEmitters.remove(emitter);
            if (removed) {
                log.info("Emitter removed for task: {}. Remaining emitters: {}", taskId, taskEmitters.size());
                // 如果该任务没有更多监听者，从主 Map 中移除列表
                if (taskEmitters.isEmpty()) {
                    emitters.remove(taskId);
                    log.info("Removed emitter list for task {} as it became empty.", taskId);
                }
            }
        }
    }

    /**
     * 定时任务，用于发送心跳以保持连接活跃.
     * 每 25 秒执行一次.
     */
    @Scheduled(fixedRate = 25000) // 25 seconds
    public void sendHeartbeats() {
        if (emitters.isEmpty()) {
            return;
        }
        log.trace("Sending heartbeats to active emitters...");
        emitters.forEach((taskId, taskEmitters) -> {
            if (!taskEmitters.isEmpty()) {
                sendHeartbeat(taskId);
            } else {
                // 清理空的列表（虽然 removeEmitter 也会做，这里是双重保险）
                emitters.remove(taskId);
                log.debug("Cleaned up empty emitter list for task {} during heartbeat schedule.", taskId);
            }
        });
    }
}
