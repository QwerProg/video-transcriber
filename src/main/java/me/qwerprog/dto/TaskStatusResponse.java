package me.qwerprog.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.qwerprog.service.SseService;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Lombok 注解，方便构建对象
public class TaskStatusResponse {
    private String status; // e.g., "processing", "completed", "error"
    private int progress;  // 0-100
    private String message;
    private String script;
    private String summary;
    private String translation;
    private String error;
    private String url;
    private String videoTitle;
    private String scriptPath;
    private String summaryPath;
    private String translationPath;
    private String detectedLanguage;
    private String summaryLanguage;
    // ... 可以添加更多需要的字段 ...

    // 用于SSE心跳
    private String type;

    // 新增的构造函数，用于从 TaskInfo 创建 TaskStatusResponse
    public TaskStatusResponse(TaskInfo taskInfo) {
        if (taskInfo != null) {
            this.status = taskInfo.getStatus().name().toLowerCase();
            this.progress = taskInfo.getProgress();
            this.message = taskInfo.getMessage();
            this.error = taskInfo.getError();
            this.url = taskInfo.getUrl();
            this.videoTitle = taskInfo.getVideoTitle();
            this.scriptPath = taskInfo.getScriptPath();
            this.summaryPath = taskInfo.getSummaryPath();
            this.translationPath = taskInfo.getTranslationPath();
            this.detectedLanguage = taskInfo.getDetectedLanguage();
            this.summaryLanguage = taskInfo.getSummaryLanguage();
            // script, summary, translation 字段通常在需要时单独加载和设置，
            // 而不是在每次状态更新时都发送完整内容。
            // 这里保持为 null，除非你有特定逻辑来填充它们。
        }
    }


    // 辅助方法，将 Map 转换为对象
    public static TaskStatusResponse fromMap(Map<String, Object> map) {
        // 注意类型转换和 null 检查
        return TaskStatusResponse.builder()
                .status((String) map.getOrDefault("status", "unknown"))
                .progress((Integer) map.getOrDefault("progress", 0))
                .message((String) map.getOrDefault("message", ""))
                .script((String) map.get("script"))
                .summary((String) map.get("summary"))
                .translation((String) map.get("translation"))
                .error((String) map.get("error"))
                .url((String) map.get("url"))
                .videoTitle((String) map.get("videoTitle"))
                .scriptPath((String) map.get("scriptPath"))
                .summaryPath((String) map.get("summaryPath"))
                .translationPath((String) map.get("translationPath"))
                .detectedLanguage((String) map.get("detectedLanguage"))
                .summaryLanguage((String) map.get("summaryLanguage"))
                .type((String) map.get("type"))
                .build();
    }

    // 转换为 Map，用于存储或 SSE 发送
    public Map<String, Object> toMap() {
        // 使用 Jackson 或手动转换
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.convertValue(this, Map.class);
    }

    // 将对象转换为 JSON 字符串
    public String toJsonString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // 在实际应用中，这里应该有更健壮的错误处理
            // 但对于 DTO 转换，通常不会失败
            return "{\"error\":\"Failed to serialize to JSON\"}";
        }
    }

    // 生成心跳消息的 JSON 字符串
    public static String heartbeatJson() {
        TaskStatusResponse heartbeat = TaskStatusResponse.builder()
                .type("heartbeat")
                .message("ping")
                .build();
        return heartbeat.toJsonString();
    }
}