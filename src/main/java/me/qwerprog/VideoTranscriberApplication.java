package me.qwerprog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring Boot 启动类.
 */
@SpringBootApplication
@EnableAsync // 启用异步方法执行
public class VideoTranscriberApplication {

    public static final Path TEMP_DIR = Paths.get("temp"); // 定义临时目录

    public static void main(String[] args) {
        createTempDirectory();
        SpringApplication.run(VideoTranscriberApplication.class, args);
        System.out.println("\n✅ Video Transcriber started successfully!");
        System.out.println("✅ 视频转录器启动成功!");
        System.out.println("\n➡️ Access the application at: http://localhost:8080");
        System.out.println("➡️ 访问该应用程序: http://localhost:8080");
        System.out.println("ℹ️ Temporary files will be stored in: " + TEMP_DIR.toAbsolutePath());

    }

    /**
     * 创建临时目录（如果不存在）.
     */
    private static void createTempDirectory() {
        if (!Files.exists(TEMP_DIR)) {
            try {
                Files.createDirectories(TEMP_DIR);
                System.out.println("✅ Created temporary directory: " + TEMP_DIR.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("❌ Failed to create temporary directory: " + TEMP_DIR.toAbsolutePath());
                e.printStackTrace();
                System.exit(1); // 无法创建临时目录则退出
            }
        }
    }
}
