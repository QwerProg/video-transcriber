"""Whisper 语音转录模块，作为无字幕视频的兜底方案"""

import os
import logging
from typing import Optional

from faster_whisper import WhisperModel

logger = logging.getLogger(__name__)


class Transcriber:
    """音频转录器，使用 faster-whisper 进行语音转文字"""

    def __init__(self, model_size: str = "base"):
        self.model_size = model_size
        self.model: Optional[WhisperModel] = None
        self.last_detected_language: Optional[str] = None

    def _load_model(self):
        if self.model is None:
            logger.info(f"正在加载 Whisper 模型: {self.model_size}")
            self.model = WhisperModel(self.model_size, device="cpu", compute_type="int8")
            logger.info("Whisper 模型加载完成")

    async def transcribe(self, audio_path: str, language: Optional[str] = None) -> dict:
        """
        转录音频文件，返回与 SubtitleExtractor.extract() 兼容的格式:
        {
            "has_subtitle": True,
            "language": str,
            "subtitle_type": "whisper",
            "segments": [{"start": float, "end": float, "text": str}, ...],
            "full_text": str
        }
        """
        if not os.path.exists(audio_path):
            raise FileNotFoundError(f"音频文件不存在: {audio_path}")

        self._load_model()

        import asyncio

        def _do_transcribe():
            return self.model.transcribe(
                audio_path,
                language=language,
                beam_size=5,
                best_of=5,
                temperature=[0.0, 0.2, 0.4],
                vad_filter=True,
                vad_parameters={"min_silence_duration_ms": 900, "speech_pad_ms": 300},
                no_speech_threshold=0.7,
                compression_ratio_threshold=2.3,
                log_prob_threshold=-1.0,
                condition_on_previous_text=False,
            )

        segments, info = await asyncio.to_thread(_do_transcribe)

        self.last_detected_language = info.language
        logger.info(
            f"Whisper 转录完成，检测语言: {info.language} (概率: {info.language_probability:.2f})"
        )

        seg_list = []
        for seg in segments:
            text = seg.text.strip()
            if not text:
                continue
            seg_list.append({
                "start": round(seg.start, 2),
                "end": round(seg.end, 2),
                "text": text,
            })

        full_text = " ".join(s["text"] for s in seg_list)

        return {
            "has_subtitle": True,
            "language": info.language,
            "subtitle_type": "whisper",
            "segments": seg_list,
            "full_text": full_text,
        }

    def get_detected_language(self) -> Optional[str]:
        return self.last_detected_language
