package me.qwerprog.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * 文件操作工具类.
 */
public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 将字符串内容保存到指定文件.
     * @param content 要保存的内容
     * @param filePath 目标文件路径
     * @throws IOException 如果写入文件失败
     */
    public static void saveStringToFile(String content, Path filePath) throws IOException {
        if (content == null) {
            log.warn("Attempted to save null content to file: {}", filePath);
            content = ""; // 保存空文件而不是抛出异常
        }
        try {
            // 确保父目录存在
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            // 使用 UTF-8 编码写入文件，覆盖已存在的文件
            Files.writeString(filePath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            log.debug("Successfully saved content to file: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save content to file {}: {}", filePath, e.getMessage(), e);
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    /**
     * 在指定目录中查找具有特定前缀和任一指定扩展名的文件.
     * @param directory 搜索目录
     * @param prefix 文件名前缀
     * @param extensions 可接受的文件扩展名 (例如 ".m4a", ".mp3")
     * @return 找到的第一个文件的 Path，如果未找到则返回 null
     */
    public static Path findFileByPrefixAndExtension(Path directory, String prefix, String... extensions) {
        if (!Files.isDirectory(directory)) {
            log.warn("Directory not found or not a directory: {}", directory);
            return null;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        if (!fileName.startsWith(prefix)) {
                            return false;
                        }
                        for (String ext : extensions) {
                            if (fileName.endsWith(ext)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .findFirst() // 找到第一个匹配的文件
                    .orElse(null); // 未找到则返回 null
        } catch (IOException e) {
            log.error("Error listing files in directory {}: {}", directory, e.getMessage(), e);
            return null;
        }
    }
}
