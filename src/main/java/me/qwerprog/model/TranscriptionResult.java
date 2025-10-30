package me.qwerprog.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 封装一次完整转录操作的结果。
 * 这是一个内部领域模型，与 DTO 分离。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResult {

    /**
     * 检测到的语言代码 (例如 "en", "zh")。
     */
    private String detectedLanguage;

    /**
     * 包含所有时间戳的转录片段列表。
     */
    private List<TranscriptionSegment> segments;

    /**
     * 将所有片段的文本合并为一个完整的转录文本。
     * @return 完整的转录字符串
     */
    public String getFullTranscript() {
        StringBuilder sb = new StringBuilder();
        for (TranscriptionSegment segment : segments) {
            sb.append(segment.getText().trim()).append(" ");
        }
        return sb.toString().trim();
    }
}
