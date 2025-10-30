package me.qwerprog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.concurrent.Future;

/**
 * 存储任务详细信息的数据传输对象.
 * 实现 Serializable 以便潜在的持久化或缓存.
 */
@Data // Lombok
@NoArgsConstructor // 需要无参构造函数用于 JSON 反序列化
public class TaskInfo implements Serializable {

    private static final long serialVersionUID = 1L; // 用于序列化版本控制

    public enum TaskStatus {
        PROCESSING, // 正在处理
        COMPLETED,  // 已完成
        ERROR       // 出现错误
    }

    private String taskId;
    private String url; // 处理的视频 URL
    private volatile TaskStatus status = TaskStatus.PROCESSING; // 任务状态，volatile 保证可见性
    private volatile int progress = 0; // 进度百分比，volatile
    private volatile String message = "任务已创建..."; // 当前状态消息，volatile
    private String error = null; // 错误信息

    // --- 结果信息 ---
    private String videoTitle = null; // 视频标题
    private String detectedLanguage = null; // 检测到的原始语言
    private String summaryLanguage = null; // 请求的摘要语言

    // 使用 String 存储文件路径，以便序列化
    private String scriptPath = null; // 优化后的转录稿文件路径
    private String summaryPath = null; // 摘要文件路径
    private String translationPath = null; // 翻译文件路径 (如果生成)
    private String rawScriptPath = null; // 原始转录稿文件路径

    // --- 运行时信息 (不建议序列化) ---
    // 使用 transient 关键字标记不需要序列化的字段
    @JsonIgnore
    private transient Future<?> taskFuture = null; // 关联的异步任务 Future，用于取消
    @JsonIgnore
    private transient String audioFilePath = null; // 临时的音频文件路径

    // Lombok 会生成 getter/setter

    /**
     * 更新任务状态和消息.
     * @param status 新状态
     * @param message 新消息
     */
    public synchronized void updateStatus(TaskStatus status, String message) {
        this.status = status;
        this.message = message;
        if (status == TaskStatus.PROCESSING) {
            this.error = null; // 清除之前的错误信息
        }
    }

    /**
     * 更新任务进度和消息.
     * @param progress 新进度
     * @param message 新消息
     */
    public synchronized void updateProgress(int progress, String message) {
        // 保证进度只增不减 (除非状态重置)
        if (progress >= this.progress) {
            this.progress = Math.min(progress, 100); // 确保不超过 100
        }
        this.message = message;
        this.status = TaskStatus.PROCESSING; // 只要有进度更新，就认为是处理中
        this.error = null;
    }

    /**
     * 标记任务完成.
     * @param message 完成消息
     */
    public synchronized void complete(String message) {
        this.status = TaskStatus.COMPLETED;
        this.progress = 100;
        this.message = message;
        this.error = null;
    }

    /**
     * 标记任务失败.
     * @param error 错误信息
     */
    public synchronized void fail(String error, String message) {
        this.status = TaskStatus.ERROR;
        this.message = message != null ? message : "处理失败";
        this.error = error;
        // 失败时进度可以保持不变，或者设为 100 表示处理结束
        // this.progress = 100;
    }

    // --- 结果设置方法 ---
    public synchronized void setResultPaths(Path scriptPath, Path summaryPath, Path translationPath, Path rawScriptPath) {
        this.scriptPath = scriptPath != null ? scriptPath.toString() : null;
        this.summaryPath = summaryPath != null ? summaryPath.toString() : null;
        this.translationPath = translationPath != null ? translationPath.toString() : null;
        this.rawScriptPath = rawScriptPath != null ? rawScriptPath.toString() : null;
    }

    public synchronized void setVideoDetails(String title, String detectedLang, String summaryLang) {
        this.videoTitle = title;
        this.detectedLanguage = detectedLang;
        this.summaryLanguage = summaryLang;
    }

}
