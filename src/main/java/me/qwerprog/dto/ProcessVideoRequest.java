package me.qwerprog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 处理视频请求的数据传输对象.
 */
@Data // Lombok 注解，自动生成 getter, setter, toString 等
public class ProcessVideoRequest {

    @NotBlank(message = "Video URL cannot be blank")
    // 添加一个基础的 URL 格式校验，但不严格，因为 yt-dlp 支持很多非标准 URL
    @Pattern(regexp = "^(http|https)://.*", message = "Invalid URL format")
    private String url;

    @NotEmpty(message = "Summary language cannot be empty")
    private String summaryLanguage = "zh"; // 默认中文
}
