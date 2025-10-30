package me.qwerprog.controller;

import me.qwerprog.dto.ProcessVideoRequest;
import me.qwerprog.dto.TaskInfo;
import me.qwerprog.service.TaskManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 处理视频处理请求的控制器.
 */
@RestController
@RequestMapping("/api")
public class VideoProcessingController {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingController.class);

    private final TaskManagementService taskManagementService;

    public VideoProcessingController(TaskManagementService taskManagementService) {
        this.taskManagementService = taskManagementService;
    }

    /**
     * 接收视频 URL 并启动处理任务.
     * @param request 包含视频 URL 和摘要语言的请求体
     * @return 包含任务 ID 的响应
     */
    @PostMapping("/process-video")
    public ResponseEntity<Map<String, String>> processVideo(@Valid @ModelAttribute ProcessVideoRequest request) { // Use @ModelAttribute for form data
        log.info("Received video processing request for URL: {}, Language: {}", request.getUrl(), request.getSummaryLanguage());
        try {
            String taskId = taskManagementService.createAndStartTask(request.getUrl(), request.getSummaryLanguage());
            log.info("Task created with ID: {}", taskId);
            // 检查是否是已存在的任务
            String message = taskManagementService.isUrlCurrentlyProcessing(request.getUrl()) && taskManagementService.getTask(taskId).getStatus() != TaskInfo.TaskStatus.PROCESSING
                    ? "该视频正在处理中，请等待..."
                    : "任务已创建，正在处理中...";

            return ResponseEntity.ok(Map.of(
                    "task_id", taskId,
                    "message", message
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid URL provided: {}", request.getUrl(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.warn("Task creation failed: {}", e.getMessage());
            // 可能是并发问题或其他内部状态错误
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create task: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error processing video request for URL: {}", request.getUrl(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred processing the video.", e);
        }
    }
}
