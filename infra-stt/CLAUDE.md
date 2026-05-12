# infra-stt 모듈 — 가드레일

> 도메인의 `SpeechToTextPort`를 구현하는 어댑터가 사는 곳.
> **STT 전용.** 다른 외부 시스템(OAuth, S3 등)은 각자의 `infra-*` 모듈에 둔다.

## 왜 별도 모듈인가

- **릴리스 주기 격리**: Azure Speech SDK 업데이트가 JPA 마이그레이션 코드와 한 모듈에 묶이지 않는다.
- **의존성 오염 차단**: Speech SDK가 끌고 오는 네이티브/트랜지티브 의존성이 다른 인프라에 노출되지 않는다.
- **벤더 교체 용이**: "Azure → Google → Clova"는 어댑터 클래스 하나 추가/교체로 끝난다.

## 책임

```
com.debatezip.infra.stt/
└── <vendor>/                           ← 벤더별 패키지
    ├── <Vendor>SpeechProperties.java   ← @ConfigurationProperties
    ├── <Vendor>SpeechConfig.java       ← @EnableConfigurationProperties (package-private)
    └── <Vendor>SpeechToTextAdapter.java ← @Component, SpeechToTextPort 구현, public
```

현재는 `azure/` 하나. 다른 벤더가 추가되면 패키지를 새로 만들고, Spring Profile이나 한정자(`@Qualifier`)로 활성 구현체를 선택한다.

## 핵심 원칙: 벤더 SDK 타입은 이 모듈 밖으로 새지 않는다

`SpeechConfig`, `SpeechRecognizer`, `SpeechRecognitionResult` 같은 Azure SDK 클래스는 어댑터 안에서만 다룬다. 메서드 시그니처/반환 타입/예외에 노출하지 않는다.

- 어댑터 입력: `AudioInput` (도메인 record)
- 어댑터 출력: `TranscriptResult` (도메인 record)
- 어댑터 예외: `BusinessException(ErrorCode.STT_*)` — 도메인이 이해할 수 있는 형태로 변환

ArchUnit 룰 `api_should_not_depend_on_infra_internals`가 이를 강제한다. api 모듈은 `SpeechToTextPort`만 알고, Azure SDK는 알지 못한다.

## 의존성

```kotlin
dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.40.0")
}
```

- `spring-boot-starter-web`을 끌어오지 **않는다**. STT 모듈은 HTTP 서버가 아니다.
- `infra-persistence`에 의존하지 않는다. ArchUnit 룰 `stt_should_not_depend_on_persistence`가 강제한다.

## 시크릿 관리

```yaml
# application.yml
debatezip:
  stt:
    azure:
      subscription-key: ${AZURE_SPEECH_KEY}   # 환경변수 / 시크릿 매니저로만 주입
      region: koreacentral
```

- 키를 `application.yml`에 평문으로 박지 않는다.
- 로컬 개발: `application-local.yml` (gitignore) 또는 `~/.bashrc`의 `export AZURE_SPEECH_KEY=...`.
- 운영: AWS Secrets Manager / Vault / k8s Secret 등으로 환경변수 주입.

## 어댑터 작성 패턴

`AzureSpeechToTextAdapter`가 살아있는 템플릿이다. 새 벤더 추가 시:

```java
@Component
public class GoogleSpeechToTextAdapter implements SpeechToTextPort {

    private final GoogleSpeechProperties properties;

    public GoogleSpeechToTextAdapter(GoogleSpeechProperties properties) {
        this.properties = properties;
    }

    @Override
    public TranscriptResult transcribe(AudioInput audio) {
        // 1) 도메인 AudioInput → Google SDK 입력 변환
        // 2) Google SDK 호출
        // 3) Google 응답 → 도메인 TranscriptResult 변환
        // 4) 실패 시 BusinessException(ErrorCode.STT_*) 으로 변환해서 throw
    }
}
```

벤더가 둘 이상이 되면:
- 한 벤더만 활성화: `@ConditionalOnProperty(name = "debatezip.stt.vendor", havingValue = "azure")` 같은 조건부 등록.
- 둘 다 활성화 (예: 폴백): `@Primary` + `@Qualifier`로 명시적 선택.

## 들어와도 되는 것

- `SpeechToTextPort` 구현체 (`@Component`)
- 벤더별 `@ConfigurationProperties` 및 `@Configuration`
- 도메인 ↔ 벤더 SDK 변환 로직
- 벤더별 에러 → `BusinessException` 매핑

## 절대 안 되는 것

- 비즈니스 로직 — 도메인 서비스로
- `api` 모듈 import — ArchUnit이 막는다
- `infra-persistence` import — ArchUnit이 막는다
- 벤더 SDK 타입을 어댑터 밖으로 노출 (반환 타입/예외에 사용 금지)
- 시크릿 평문 하드코딩

## 테스트 전략

- **단위 테스트는 도메인 쪽에서**: `SpeechToTextPort`의 Fake 구현을 도메인 테스트에서 사용. 어댑터 자체는 외부 네트워크 의존이라 단위 테스트가 어렵다.
- **어댑터 통합 테스트**: 실제 Azure 호출 또는 SDK 모킹. CI에서는 옵션 처리 (시크릿 없으면 skip).
