package me.qwerprog.service;

import me.qwerprog.integration.YtDlpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

/**
 * 处理视频下载和转换的服务.
 */
@Service
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);
    private final YtDlpClient ytDlpClient;

    public VideoProcessingService(YtDlpClient ytDlpClient) {
        this.ytDlpClient = ytDlpClient;
    }

    /**
     * 下载视频并将其转换为音频文件.
     * @param url 视频 URL
     * @param outputDir 输出目录
     * @return 包含音频文件路径和视频标题的 Map
     * @throws RuntimeException 如果下载或转换失败
     */
    public Map<String, Path> downloadAndConvertToAudio(String url, Path outputDir) {
        log.info("Starting video download and conversion for URL: {}", url);
        try {
            // 调用 YtDlpClient 来执行下载和转换
            Map<String, Path> result = ytDlpClient.downloadAudio(url, outputDir);
            log.info("视频处理成功。音频文件: {}, 标题: {}",
                    result.get(YtDlpClient.AUDIO_PATH_KEY), result.get(YtDlpClient.TITLE_KEY));
            return result;
        } catch (Exception e) {
            log.error("无法从 URL 下载或转换视频: {}", url, e);
            throw new RuntimeException("处理视频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取视频信息（标题等）.
     * @param url 视频 URL
     * @return 包含视频信息的 Map
     * @throws RuntimeException 如果获取信息失败
     */
    public Map<String, String> getVideoInfo(String url) {
        log.info("Fetching video info for URL: {}", url);
        try {
            return ytDlpClient.getVideoInfo(url);
        } catch (Exception e) {
            log.error("Failed to get video info from URL: {}", url, e);
            throw new RuntimeException("Failed to get video info: " + e.getMessage(), e);
        }
    }
}
