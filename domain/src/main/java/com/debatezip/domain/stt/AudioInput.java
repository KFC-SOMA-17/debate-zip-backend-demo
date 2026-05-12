package com.debatezip.domain.stt;

import com.debatezip.common.exception.BusinessException;
import com.debatezip.common.exception.ErrorCode;

// STT 포트의 입력. 벤더 중립적인 표현으로 둔다.
// data는 raw 오디오 바이트, format은 컨테이너 종류, languageCode는 BCP-47 (예: "ko-KR").
public record AudioInput(byte[] data, AudioFormat format, String languageCode) {

    private static final String DEFAULT_LANGUAGE = "ko-KR";

    public static AudioInput of(byte[] data, AudioFormat format, String languageCode) {
        if (data == null || data.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "audio data must not be empty");
        }
        if (format == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "audio format must not be null");
        }
        String lang = (languageCode == null || languageCode.isBlank()) ? DEFAULT_LANGUAGE : languageCode;
        return new AudioInput(data, format, lang);
    }

    public enum AudioFormat {
        WAV_PCM_16K_MONO, // 16kHz, 16-bit, mono — STT 표준
        MP3,
        OGG_OPUS
    }
}
