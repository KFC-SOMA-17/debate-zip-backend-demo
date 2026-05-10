# api 모듈 — 가드레일

> Spring Boot 진입점. HTTP 어댑터 역할만 담당하며, 모든 모듈을 끌어와 DI 조립을 수행한다.
> **비즈니스 로직을 직접 작성하지 않는다.** 작성하고 싶은 충동이 들면 그건 도메인/application으로 옮겨야 한다는 신호다.

## 책임

```
com.debatezip/
├── DebatezipApplication.java       ← @SpringBootApplication (com.debatezip 패키지에 위치 → 자동 스캔이 모든 모듈 커버)
└── api/
    ├── GlobalExceptionHandler.java ← BusinessException → HTTP 응답 변환
    ├── arch/ArchitectureTest.java  ← 의존성 룰 강제 (test)
    ├── auth/                       ← (필요 시) JWT, SecurityConfig
    └── <도메인>/
        ├── <도메인>Controller.java
        └── <도메인>Dtos.java
```

## 컨트롤러 작성 규칙

```java
@RestController
@RequestMapping("/debates")
public class DebateController {
    private final DebateService debateService;  // ← Service만 주입

    public DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @PostMapping
    public DebateResponse create(@RequestBody CreateDebateRequest request) {
        Debate debate = debateService.create(request.title(), request.description());
        return DebateResponse.from(debate);
    }
}
```

**규칙:**
- `DebateService` (또는 `application` 모듈의 ApplicationService)만 주입한다.
- `DebateRepository` 같은 Port 인터페이스도 직접 주입 **금지** (ArchUnit이 막는다).
- `DebateJpaEntity`, `DebateJpaRepository`는 import 자체가 ArchUnit으로 차단된다.

## DTO 규칙

요청/응답 DTO는 도메인 객체와 **다른 클래스**다. 같은 record라도 별개로 둔다.

```java
public record CreateDebateRequest(String title, String description) {}

public record DebateResponse(Long id, String title, String description) {
    public static DebateResponse from(Debate debate) {
        return new DebateResponse(debate.id(), debate.title(), debate.description());
    }
}
```

이렇게 분리하면:
- 응답 스키마 변경이 도메인을 흔들지 않는다
- 도메인 변경이 클라이언트 호환성을 깨뜨리지 않는다 (DTO 매핑만 조정)

`*Dtos.java` 한 파일에 같은 도메인의 모든 DTO를 모으는 패턴을 권장 (good-example의 `OrderDtos.kt` 참고).

## 예외 → HTTP 변환

도메인이 던지는 `BusinessException`은 `GlobalExceptionHandler`에서 받아 `ErrorCode.getStatus()`(int)를 `HttpStatus`로 매핑한다.

새 에러 응답 형식이 필요하면 여기서만 수정 — 도메인은 건드리지 않는다.

## 의존성

```kotlin
plugins {
    id("org.springframework.boot")  // ← api 모듈만 적용
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
}
```

ArchUnit 1.4.2 미만은 Java 25 클래스 파일을 못 읽는다 — 다운그레이드 금지.

## ArchUnit 룰 위치

`src/test/java/com/debatezip/api/arch/ArchitectureTest.java`. 새 룰을 추가할 때:
- `noClasses().that().resideInAPackage(...)` 패턴
- 룰 이름은 영문 snake_case (실패 메시지에 그대로 노출)
- 룰을 일시 우회(`allowEmptyShould(true)`)하지 말 것 — 룰이 잡는 건 거의 항상 진짜 설계 문제

## 들어와도 되는 것

- 컨트롤러 (`@RestController`)
- 요청/응답 DTO
- `@RestControllerAdvice` 예외 핸들러
- `SecurityConfig`, `WebConfig` 같은 Spring 설정 클래스
- 인증 필터 (`OncePerRequestFilter`)
- ArchUnit 테스트

## 절대 안 되는 것

- 비즈니스 로직 — 도메인/`application`으로
- Repository 직접 주입 — `*Service` 통해서만
- JPA 엔티티 import — ArchUnit이 막는다
- 도메인 객체를 그대로 응답으로 반환 — 항상 DTO 변환

## application 모듈을 만들어야 하는 시점

현재는 4모듈 MVP라 `application`이 없다. 다음 신호가 보이면 신설:
- 컨트롤러가 두 개 이상의 도메인 서비스를 직접 주입받기 시작한다
- 도메인 서비스가 다른 도메인 서비스를 호출하고 싶어한다 (순환 참조 위험)

이 경우 `application` 모듈을 추가하고:
- 컨트롤러 → ApplicationService → 여러 DomainService 조합
- 도메인 서비스끼리는 여전히 서로 모름

`settings.gradle.kts`에 `"application"` 추가 → `application/build.gradle.kts`에서 도메인 모듈들 implementation → `api/build.gradle.kts`에 `implementation(project(":application"))` 추가.
