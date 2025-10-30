package me.qwerprog.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WhisperClient {
    private static final Logger log = LoggerFactory.getLogger(WhisperClient.class);

    public static final String TRANSCRIPT_KEY = "transcript";
    public static final String DETECTED_LANGUAGE_KEY = "detected_language";

    @Value("${whisper.cpp.path:/usr/local/bin/whisper}")
    private String whisperExecutablePath;

    @Value("${whisper.cpp.model:/usr/local/share/whisper/ggml-base.bin}")
    private String modelPath;

    public Map<String, String> transcribe(Path audioPath, String language) throws IOException {
        log.info("Transcribing audio file: {} using whisper.cpp", audioPath);

        List<String> command = new ArrayList<>();
        command.add(whisperExecutablePath);
        command.add("-m");
        command.add(modelPath);
        command.add("-f");
        command.add(audioPath.toString());
        command.add("--output-txt");

        if (language != null && !language.isEmpty()) {
            command.add("-l");
            command.add(language);
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("whisper.cpp output: {}", line);
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("whisper.cpp failed with exit code: " + exitCode);
            }

            // 读取生成的文本文件
            Path outputTxtPath = audioPath.resolveSibling(
                    audioPath.getFileName().toString() + ".txt"
            );

            String transcript;
            if (Files.exists(outputTxtPath)) {
                transcript = Files.readString(outputTxtPath).trim();
                // 清理临时文件
                Files.deleteIfExists(outputTxtPath);
            } else {
                transcript = output.toString().trim();
            }

            log.info("Transcription completed successfully");
            return Map.of(
                    TRANSCRIPT_KEY, transcript,
                    DETECTED_LANGUAGE_KEY, language != null ? language : "auto"
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Transcription was interrupted", e);
        }
    }
}
