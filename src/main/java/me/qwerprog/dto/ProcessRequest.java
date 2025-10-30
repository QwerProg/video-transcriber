package me.qwerprog.dto;

import lombok.Data;

@Data // Lombok 注解，自动生成 getter/setter/toString 等
public class ProcessRequest {
    private String url;
    private String summaryLanguage = "zh"; // 默认值
}