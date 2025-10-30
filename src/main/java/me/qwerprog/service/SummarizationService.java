package me.qwerprog.service;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    private final WebClient webClient;
    private final String apiKey;
    private static final String GEMINI_MODEL = "gemini-pro";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public SummarizationService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;

        if (!StringUtils.hasText(apiKey) || apiKey.equals("YOUR_GOOGLE_AI_API_KEY_HERE")) {
            log.warn("Gemini API key is not configured. Summarization feature will be disabled.");
            this.webClient = null;
        } else {
            this.webClient = WebClient.builder()
                    .baseUrl(API_URL)
                    .build();
            log.info("Gemini REST client initialized successfully for SummarizationService.");
        }
    }

    public String optimizeTranscript(String rawScript) {
        if (webClient == null) {
            log.warn("Gemini client not available. Returning raw script.");
            return rawScript;
        }
        if (!StringUtils.hasText(rawScript)) {
            return rawScript;
        }

        log.info("Optimizing transcript using Gemini...");

        String prompt = """
            请优化以下视频转录文本，使其更易读、流畅，同时保持原意：
            
            要求：
            - 修正语法错误和标点符号
            - 合并相似的句子，使段落连贯
            - 保持原始内容的完整性，不添加新信息
            - 使用 Markdown 格式
            - 不要添加标题或注释
            
            转录文本：
            ```
            %s
            ```
            """.formatted(rawScript);

        return callGeminiApi(prompt, 0.3f, 4000);
    }

    public String summarize(String text, String targetLanguage, String videoTitle) {
        if (webClient == null) {
            log.warn("Gemini client not available. Cannot generate summary.");
            return "摘要功能暂不可用";
        }
        if (!StringUtils.hasText(text)) {
            return "无内容可摘要";
        }

        log.info("Generating summary for '{}' in language: {}", videoTitle, targetLanguage);

        String languageName = getLanguageName(targetLanguage);
        String prompt = """
            请为以下视频内容生成详细的%s摘要：
            
            视频标题：%s
            
            要求：
            - 使用 Markdown 格式
            - 包含主要观点和关键信息
            - 结构清晰，使用标题和列表
            - 长度约 300-500 字
            - 使用%s
            
            内容：
            ```
            %s
            ```
            """.formatted(languageName, videoTitle, languageName, text);

        return callGeminiApi(prompt, 0.5f, 2000);
    }

    private String callGeminiApi(String prompt, float temperature, int maxTokens) {
        if (webClient == null) {
            log.warn("Gemini client not initialized. API call skipped.");
            return null; // 或者返回一个默认值
        }

        JsonObject requestBody = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contentsArray.add(content);
        requestBody.add("contents", contentsArray);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", temperature);
        generationConfig.addProperty("maxOutputTokens", maxTokens);
        requestBody.add("generationConfig", generationConfig);

        String response = null;
        try {
            response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(GEMINI_MODEL + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // block() 会在主线程上等待结果，但在异步任务中 (taskExecutor-1) 这是可以的

            // ---------------------------------------------------------------
            // 重点修改：在这里添加了健壮的解析和错误处理
            // ---------------------------------------------------------------
            if (!StringUtils.hasText(response)) {
                log.error("Gemini API call returned an empty response.");
                return "API 调用失败: 响应为空";
            }

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

            // 1. 优先检查 API 是否返回了错误
            if (jsonResponse.has("error")) {
                JsonObject error = jsonResponse.getAsJsonObject("error");
                String errorMessage = error.has("message") ? error.get("message").getAsString() : "Unknown API error";
                log.error("Gemini API returned an error. Response: {}", response);
                return "API 调用失败: " + errorMessage;
            }

            // 2. 检查 'candidates' 数组是否存在且不为空
            if (!jsonResponse.has("candidates") || !jsonResponse.get("candidates").isJsonArray() || jsonResponse.getAsJsonArray("candidates").isEmpty()) {

                // 检查是否是因为内容安全被阻止
                if (jsonResponse.has("promptFeedback")) {
                    log.warn("Gemini API blocked the response due to content filters. Response: {}", response);
                    return "API 调用失败: 内容可能被安全策略阻止。";
                }

                log.error("Gemini API returned an unexpected response (no 'candidates'). Response: {}", response);
                return "API 调用失败: 未返回有效内容。";
            }

            // 3. 安全地解析响应
            // 我们将原来的长链条调用放在 try-catch 中，以防止 "content" 或 "parts" 缺失
            try {
                String result = jsonResponse.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();

                log.info("API call successful.");
                return result;

            } catch (Exception e) {
                log.error("Failed to parse Gemini API's successful response. Response: '{}', Error: {}", response, e.getMessage(), e);
                return "API 调用失败: 解析响应结构失败。";
            }
            // ---------------------------------------------------------------
            // 修改结束
            // ---------------------------------------------------------------

        } catch (Exception e) {
            // 这个 catch 块捕获 WebClient 的网络错误或 block() 错误
            log.error("Gemini API call failed during network request: {}", e.getMessage(), e);
            // 如果 response 不是 null，说明错误发生在解析阶段之前，但我们还是记录一下
            if (response != null) {
                log.error("Raw response before error: {}", response);
            }
            return "API 调用失败: " + e.getMessage();
        }
    }

    private String getLanguageName(String languageCode) {
        return switch (languageCode) {
            case "zh" -> "中文（简体）";
            case "en" -> "English";
            case "es" -> "Español";
            case "fr" -> "Français";
            case "de" -> "Deutsch";
            case "ja" -> "日本語";
            case "ko" -> "한국어";
            default -> languageCode;
        };
    }
}
