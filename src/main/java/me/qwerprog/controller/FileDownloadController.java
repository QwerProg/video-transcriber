package me.qwerprog.controller;

import me.qwerprog.service.TaskManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static me.qwerprog.VideoTranscriberApplication.TEMP_DIR;

/**
 * 处理文件下载请求的控制器.
 */
@RestController
@RequestMapping("/api")
public class FileDownloadController {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadController.class);
    // 简单的文件名验证，防止路径遍历
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[\\w\\-.]+$");


    public FileDownloadController() {
        // Constructor
    }

    /**
     * 下载指定的文件.
     * @param filename 要下载的文件名
     * @return 包含文件内容的 ResponseEntity
     */
    @GetMapping("/download/{filename:.+}") // :.+ 允许文件名包含点号
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        log.info("File download requested: {}", filename);

        // --- 安全性检查 ---
        // 1. 文件名格式校验
        if (!SAFE_FILENAME_PATTERN.matcher(filename).matches() || filename.contains("..")) {
            log.warn("Invalid filename requested for download: {}", filename);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename format.");
        }
        // 2. 检查文件扩展名 (简单检查)
        if (!filename.toLowerCase().endsWith(".md")) {
            log.warn("Attempted to download non-markdown file: {}", filename);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .md files can be downloaded.");
        }

        try {
            Path file = TEMP_DIR.resolve(filename).normalize(); // 解析并规范化路径

            // 3. 确保文件仍在 temp 目录下
            if (!file.startsWith(TEMP_DIR.toAbsolutePath())) {
                log.warn("Attempted path traversal download: {}", filename);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path.");
            }

            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("Serving file: {}", file.toAbsolutePath());
                String contentType = "text/markdown"; // 直接指定 Markdown 类型

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.warn("File not found or not readable: {}", file.toAbsolutePath());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found or cannot be read.");
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL exception for file: {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating file URL.", e);
        } catch (ResponseStatusException e) {
            throw e; // 直接抛出已创建的异常
        } catch (Exception e) {
            log.error("Error during file download for: {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while downloading the file.", e);
        }
    }
}
