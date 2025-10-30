package me.qwerprog.controller;

import me.qwerprog.dto.TaskInfo;
import me.qwerprog.service.SseService;
import me.qwerprog.service.TaskManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 提供任务状态查询和 SSE 流的控制器.
 */
@RestController
@RequestMapping("/api")
public class TaskStatusController {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusController.class);

    private final TaskManagementService taskManagementService;
    private final SseService sseService;


    public TaskStatusController(TaskManagementService taskManagementService, SseService sseService) {
        this.taskManagementService = taskManagementService;
        this.sseService = sseService;
    }

    /**
     * 获取指定任务的当前状态.
     * @param taskId 任务 ID
     * @return 任务信息
     */
    @GetMapping("/task-status/{taskId}")
    public ResponseEntity<TaskInfo> getTaskStatus(@PathVariable String taskId) {
        log.debug("Request received for task status: {}", taskId);
        try {
            TaskInfo taskInfo = taskManagementService.getTask(taskId);
            return ResponseEntity.ok(taskInfo);
        } catch (IllegalArgumentException e) {
            log.warn("Task not found for status request: {}", taskId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found", e);
        }
    }

    /**
     * 提供任务状态的 SSE 流.
     * @param taskId 任务 ID
     * @return SseEmitter 用于流式传输事件
     */
    @GetMapping(value = "/task-stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter taskStream(@PathVariable String taskId) {
        log.info("SSE connection requested for task: {}", taskId);
        try {
            // 检查任务是否存在，如果不存在会抛出异常
            taskManagementService.getTask(taskId);
            return sseService.createEmitter(taskId);
        } catch (IllegalArgumentException e) {
            log.warn("Attempted to stream status for non-existent task: {}", taskId);
            // 不能直接在 SseEmitter 方法中抛出 ResponseStatusException，需要特殊处理
            // 返回一个立即完成并带有错误的 SseEmitter
            SseEmitter errorEmitter = new SseEmitter(0L); // Timeout immediately
            try {
                // 在 emitter 内部完成并报告错误可能更符合 SSE 规范
                // 但这里简单处理，因为客户端通常会处理连接失败
                errorEmitter.completeWithError(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found", e));
            } catch (Exception ignored) {}
            return errorEmitter;
        } catch (Exception e) {
            log.error("Error creating SSE stream for task: {}", taskId, e);
            SseEmitter errorEmitter = new SseEmitter(0L);
            try {
                errorEmitter.completeWithError(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating SSE stream", e));
            } catch (Exception ignored) {}
            return errorEmitter;
        }
    }

    // 可选: 添加删除任务的端点
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        log.info("Request to delete task: {}", taskId);
        try {
            taskManagementService.cancelAndDeleteTask(taskId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Attempted to delete non-existent task: {}", taskId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found", e);
        } catch (Exception e) {
            log.error("Error deleting task: {}", taskId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting task", e);
        }
    }
}
