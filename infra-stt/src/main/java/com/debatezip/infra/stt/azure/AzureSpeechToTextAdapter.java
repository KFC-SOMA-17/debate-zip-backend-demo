package com.debatezip.infra.stt.azure;

import com.debatezip.common.exception.BusinessException;
import com.debatezip.common.exception.ErrorCode;
import com.debatezip.domain.stt.AudioInput;
import com.debatezip.domain.stt.SpeechToTextPort;
import com.debatezip.domain.stt.TranscriptResult;
import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamContainerFormat;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Component;

@Component
public class AzureSpeechToTextAdapter implements SpeechToTextPort {

    private final AzureSpeechProperties properties;

    public AzureSpeechToTextAdapter(AzureSpeechProperties properties) {
        this.properties = properties;
    }

    @Override
    public TranscriptResult transcribe(AudioInput audio) {
        // SDK 객체는 모두 AutoCloseable. try-with-resources로 네이티브 리소스 누수를 방지한다.
        try (SpeechConfig speechConfig = SpeechConfig.fromSubscription(
                properties.subscriptionKey(), properties.region())) {

            speechConfig.setSpeechRecognitionLanguage(audio.languageCode());

            try (PushAudioInputStream pushStream = AudioInputStream.createPushStream(toAzureFormat(audio.format()));
                 AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);
                 SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {

                pushStream.write(audio.data());
                pushStream.close(); // EOS 신호. 이걸 호출해야 recognizeOnceAsync가 완료된다.

                SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get();
                try {
                    return toDomain(result);
                } finally {
                    result.close();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.STT_CANCELED, "STT thread interrupted");
        } catch (ExecutionException ex) {
            // 비동기 작업 내부 예외 — Azure SDK 호출 실패, 네트워크, 인증 오류 등
            throw new BusinessException(ErrorCode.STT_RECOGNITION_FAILED, ex.getCause() == null
                    ? ex.getMessage() : ex.getCause().getMessage());
        }
    }

    // 도메인 AudioFormat → Azure AudioStreamFormat.
    // 변환은 어댑터 안에만 둔다. 도메인은 Azure 타입을 알 필요가 없다.
    private AudioStreamFormat toAzureFormat(AudioInput.AudioFormat format) {
        return switch (format) {
            case WAV_PCM_16K_MONO -> AudioStreamFormat.getDefaultInputFormat();
            case MP3 -> AudioStreamFormat.getCompressedFormat(AudioStreamContainerFormat.MP3);
            case OGG_OPUS -> AudioStreamFormat.getCompressedFormat(AudioStreamContainerFormat.OGG_OPUS);
        };
    }

    private TranscriptResult toDomain(SpeechRecognitionResult result) {
        return switch (result.getReason()) {
            case RecognizedSpeech -> TranscriptResult.of(
                    result.getText(),
                    extractConfidence(result),
                    toDuration(result.getDuration())
            );
            case NoMatch -> TranscriptResult.empty();
            case Canceled -> {
                CancellationDetails details = CancellationDetails.fromResult(result);
                throw new BusinessException(
                        ErrorCode.STT_CANCELED,
                        details.getReason() + ": " + details.getErrorDetails()
                );
            }
            default -> throw new BusinessException(
                    ErrorCode.STT_RECOGNITION_FAILED,
                    "unexpected reason: " + result.getReason()
            );
        };
    }

    // Azure SDK는 duration을 100-nanosecond tick 단위 BigInteger로 반환한다 (.NET 호환).
    // 도메인은 표준 java.time.Duration만 안다 — 변환은 어댑터 안에서.
    private Duration toDuration(BigInteger ticks) {
        if (ticks == null) {
            return Duration.ZERO;
        }
        return Duration.ofNanos(ticks.longValueExact() * 100L);
    }

    // Azure는 JSON 형태로 상세 결과를 제공한다 (옵션 활성화 시).
    // 여기서는 간단히 기본값을 반환. 실제 운영에서는 detailedResult 파싱 또는 NBest 활용.
    private double extractConfidence(SpeechRecognitionResult result) {
        String json = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
        if (json == null || json.isBlank()) {
            return 1.0;
        }
        // TODO: NBest[0].Confidence 파싱. 도메인에 노출하는 타입은 그대로 유지한다.
        return 1.0;
    }
}
