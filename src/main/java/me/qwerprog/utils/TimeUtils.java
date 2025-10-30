package me.qwerprog.utils;

import java.util.Locale;

/**
 * 时间处理工具类
 */
public class TimeUtils {

    /**
     * 将秒数格式化为 SRT 字幕文件的时间戳格式 (HH:mm:ss,SSS)
     *
     * @param totalSeconds 总秒数 (可以带小数)
     * @return 格式化的时间字符串，例如 "00:01:23,456"
     */
    public static String formatSrtTimestamp(double totalSeconds) {
        // 确保时间不是负数
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }

        // 计算小时、分钟、秒和毫秒
        int hours = (int) (totalSeconds / 3600);
        int remainder = (int) (totalSeconds % 3600);
        int minutes = remainder / 60;
        int seconds = remainder % 60;
        int milliseconds = (int) ((totalSeconds - (hours * 3600) - (minutes * 60) - seconds) * 1000);

        // 使用 Locale.US 确保使用点 . 作为小数点分隔符（如果需要）
        // 并使用 %02d (小时、分钟、秒) 和 %03d (毫秒) 来补零
        return String.format(Locale.US, "%02d:%02d:%02d,%03d",
                hours, minutes, seconds, milliseconds);
    }
}