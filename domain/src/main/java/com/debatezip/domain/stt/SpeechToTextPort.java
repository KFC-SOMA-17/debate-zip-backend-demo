package com.debatezip.domain.stt;

// 도메인이 요구하는 STT 동작을 인터페이스로 선언한다 (Port).
// 구현체(Adapter)는 infra-stt 모듈이 책임지며, 도메인은 어떤 벤더(Azure/Google/Clova 등)가
// 쓰이는지 알지 못한다. 벤더 교체 시 도메인 코드는 한 줄도 변경되지 않는다.
public interface SpeechToTextPort {

    TranscriptResult transcribe(AudioInput audio);
}
