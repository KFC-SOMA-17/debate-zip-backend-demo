package com.debatezip.domain.stt;

import java.time.Duration;

// STT 포트의 출력. 벤더 중립적이어야 하므로 Azure SpeechRecognitionResult 같은 외부 타입을
// 그대로 반환하지 않는다.
//
// text  : 인식된 텍스트 (NoMatch 시 빈 문자열)
// confidence : 0.0 ~ 1.0. 일부 벤더는 단어 단위만 주므로 평균/대표값을 둔다.
// audioDuration : 입력 오디오의 길이 — 과금/UI 표시에 유용
public record TranscriptResult(String text, double confidence, Duration audioDuration) {

    public static TranscriptResult empty() {
        return new TranscriptResult("", 0.0, Duration.ZERO);
    }

    public static TranscriptResult of(String text, double confidence, Duration audioDuration) {
        return new TranscriptResult(
                text == null ? "" : text,
                confidence,
                audioDuration == null ? Duration.ZERO : audioDuration
        );
    }
}
