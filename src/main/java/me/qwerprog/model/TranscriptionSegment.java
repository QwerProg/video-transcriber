package me.qwerprog.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 代表一个单独的、带时间戳的转录片段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionSegment {

    /**
     * 片段开始时间（秒）。
     */
    private double start;

    /**
     * 片段结束时间（秒）。
     */
    private double end;

    /**
     * 该片段的转录文本。
     */
    private String text;
}
