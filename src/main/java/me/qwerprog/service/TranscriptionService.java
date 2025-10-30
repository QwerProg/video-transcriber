package me.qwerprog.service;

import me.qwerprog.integration.WhisperClient; // 假设有一个 WhisperClient
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

/**
 * 处理音频转录的服务.
 */
@Service
public class TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);
    private final WhisperClient whisperClient; // 注入 Whisper 客户端

    // 假设 WhisperClient 是存在的
    public TranscriptionService(WhisperClient whisperClient) {
        this.whisperClient = whisperClient;
    }
    // 如果没有 WhisperClient，可以先提供一个无参构造函数或注释掉构造函数
    // public TranscriptionService() {
    //     log.warn("WhisperClient not implemented yet. Transcription will be mocked.");
    //     this.whisperClient = null; // 或者使用一个 Mock 实现
    // }


    /**
     * 转录音频文件.
     * @param audioPath 音频文件路径
     * @param language 可选的目标语言
     * @return 包含转录文本和检测到的语言的 Map
     * @throws RuntimeException 如果转录失败
     */
    public Map<String, String> transcribeAudio(Path audioPath, String language) {
        log.info("Starting audio transcription for file: {}", audioPath);
        if (whisperClient == null) {
            log.warn("WhisperClient is not available. Returning mock transcription.");
            // 返回模拟数据
            return Map.of(
                    WhisperClient.TRANSCRIPT_KEY, "# Mock Transcription\n\nThis is a placeholder because the actual transcription service is not implemented.",
                    WhisperClient.DETECTED_LANGUAGE_KEY, "en" // 假设检测到英文
            );
        }

        try {
            // 调用 WhisperClient 执行转录
            Map<String, String> result = whisperClient.transcribe(audioPath, language);
            log.info("Transcription completed. Detected language: {}", result.get(WhisperClient.DETECTED_LANGUAGE_KEY));
            return result;
        } catch (Exception e) {
            log.error("Failed to transcribe audio file: {}", audioPath, e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }
}
