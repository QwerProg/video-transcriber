package me.qwerprog.service;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    private final WebClient webClient;
    private final String apiKey;
    private static final String GEMINI_MODEL = "gemini-pro";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final Map<String, String> LANGUAGE_MAP_PROMPT = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("zh", "中文（简体）"),
            Map.entry("es", "Español"),
            Map.entry("fr", "Français"),
            Map.entry("de", "Deutsch"),
            Map.entry("it", "Italiano"),
            Map.entry("pt", "Português"),
            Map.entry("ru", "Русский"),
            Map.entry("ja", "日本語"),
            Map.entry("ko", "한국어"),
            Map.entry("ar", "العربية")
    );

    public TranslationService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;

        if (!StringUtils.hasText(apiKey) || apiKey.equals("YOUR_GOOGLE_AI_API_KEY_HERE")) {
            log.warn("Gemini API key is not configured. Translation feature will be disabled.");
            this.webClient = null;
        } else {
            this.webClient = WebClient.builder()
                    .baseUrl(API_URL)
                    .build();
            log.info("Gemini REST client initialized successfully for TranslationService.");
        }
    }

    public boolean shouldTranslate(String sourceLanguage, String targetLanguage) {
        if (!StringUtils.hasText(sourceLanguage) || !StringUtils.hasText(targetLanguage)) {
            return false;
        }
        String srcLang = sourceLanguage.toLowerCase().strip();
        String tgtLang = targetLanguage.toLowerCase().strip();

        if (srcLang.equals(tgtLang)) {
            return false;
        }
        List<String> chineseVariants = List.of("zh", "zh-cn", "zh-hans");
        if (chineseVariants.contains(srcLang) && chineseVariants.contains(tgtLang)) {
            return false;
        }
        return true;
    }

    public String translateText(String text, String targetLanguage, String sourceLanguage) {
        if (webClient == null) {
            log.warn("Gemini client not available. Skipping translation.");
            return text;
        }
        if (!StringUtils.hasText(text) || !shouldTranslate(sourceLanguage, targetLanguage)) {
            return text;
        }

        String sourceLangName = LANGUAGE_MAP_PROMPT.getOrDefault(sourceLanguage, sourceLanguage);
        String targetLangName = LANGUAGE_MAP_PROMPT.getOrDefault(targetLanguage, targetLanguage);
        log.info("Translating text from {} to {} using Gemini...", sourceLangName, targetLangName);

        try {
            String systemPrompt = String.format("""
                你是专业翻译专家。请将以下%s文本准确、流畅地翻译为%s。
                翻译要求：
                - 严格保持原文的格式和段落结构（包括 Markdown 标记）。
                - 准确传达原意，语言自然地道。
                - 保留专业术语、人名、地名的准确性。
                - 不要添加任何解释、注释或原文。
                - 只输出翻译后的%s文本。
                """, sourceLangName, targetLangName, targetLangName);

            String userPrompt = String.format("""
                请将下面的%s文本翻译为%s：
                ```
                %s
                ```
                """, sourceLangName, targetLangName, text);

            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;

            JsonObject requestBody = new JsonObject();
            JsonArray contentsArray = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", combinedPrompt);
            parts.add(part);
            content.add("parts", parts);
            contentsArray.add(content);
            requestBody.add("contents", contentsArray);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.1f);
            generationConfig.addProperty("maxOutputTokens", 4000);
            requestBody.add("generationConfig", generationConfig);

            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(GEMINI_MODEL + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            String translatedText = jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            log.info("Translation successful.");
            return ensureMarkdownParagraphs(translatedText);

        } catch (Exception e) {
            log.error("Failed to translate text from {} to {}: {}", sourceLangName, targetLangName, e.getMessage(), e);
            return text;
        }
    }

    private String ensureMarkdownParagraphs(String text) {
        if (!StringUtils.hasText(text)) return "";
        return text.replace("\r\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }
}
