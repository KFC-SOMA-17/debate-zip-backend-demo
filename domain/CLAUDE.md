# domain 모듈 — 가드레일

> 비즈니스 규칙의 집. 인프라 기술(JPA, Mail, BCrypt 등)에 **절대로** 의존하지 않는다.
> ArchUnit 룰 `domain_should_not_depend_on_api_or_infra`가 이를 빌드 시점에 강제한다.

## 책임

도메인 모듈은 두 가지 패키지로 구성된다 (sample을 참고):

```
com.debatezip.domain.<도메인>/
├── <도메인 객체>.java         ← 순수 record/클래스, JPA 어노테이션 금지
├── <도메인>Repository.java    ← 포트 인터페이스 (구현은 infra)
├── <PortName>Port.java        ← 외부 협력자 포트 (예: NotificationPort)
└── <도메인>Service.java       ← @Service, 단일 도메인 안에서 끝나는 use case
```

## 의존성

`build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":common"))
    implementation("org.springframework:spring-context")  // @Service만
}
```

**여기에 다음을 추가하면 안 된다:**
- `spring-data-jpa`, `spring-jdbc`
- `spring-web`, `spring-webmvc`
- `jackson-*`
- 다른 도메인 모듈 (현재 단일 domain 모듈이라 해당 없음)

## 들어와도 되는 것

- 순수 Java 도메인 객체 (record 권장)
- 비즈니스 규칙 검증 로직 (생성자, factory, 도메인 메서드 안)
- 포트 인터페이스 (Repository, NotificationPort 등)
- `@Service`로 단일 도메인 use case
- 도메인 enum / value object

## 절대 안 되는 것

- `@Entity`, `@Table`, `@Id`, `@Column` 같은 JPA 어노테이션 — `infra`의 `*JpaEntity`로
- `@RestController`, `@RequestMapping` 같은 웹 어노테이션
- 다른 모듈의 import (`com.debatezip.api.*`, `com.debatezip.infra.*`)
- DB 액세스 코드 직접 작성
- HTTP 응답 형식이 도메인 메서드 시그니처에 영향을 주는 것

## 새 도메인 추가 패턴

`sample`이 살아있는 템플릿이다. 새 도메인 `Debate`를 추가한다면:

```java
// domain/src/main/java/com/debatezip/domain/debate/Debate.java
public record Debate(Long id, String title, String description) {
    public static Debate create(String title, String description) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return new Debate(null, title, description);
    }
}

// domain/src/main/java/com/debatezip/domain/debate/DebateRepository.java
public interface DebateRepository {
    Debate save(Debate debate);
    Optional<Debate> findById(Long id);
}

// domain/src/main/java/com/debatezip/domain/debate/DebateService.java
@Service
public class DebateService {
    private final DebateRepository repository;
    public DebateService(DebateRepository repository) { this.repository = repository; }
    // ...
}
```

구현체(`DebateRepositoryAdapter`)와 JPA 엔티티(`DebateJpaEntity`)는 `infra` 모듈에서.

## 두 도메인을 모두 알아야 하는 흐름이 생기면

예: `Debate.close()` 시점에 `User.notifyClosed()`도 호출해야 한다면:
1. `DebateService`나 `UserService`에 다른 도메인을 끌어들이지 **않는다**.
2. `application` 모듈을 신설하고 `DebateApplicationService`를 만든다 (현재 4모듈 MVP에는 없음).
3. 이 ApplicationService가 `DebateService`와 `UserService`를 둘 다 주입받아 조율한다.

근거: 학습 리포트의 "순환 참조가 보이면 모듈을 위로 올린다" 원칙.

## 단위 테스트

도메인 모듈은 **DB 없이** 테스트 가능해야 한다. Repository 인터페이스를 Fake 구현체로 갈아끼우면 끝.

```java
class FakeDebateRepository implements DebateRepository {
    private final Map<Long, Debate> store = new HashMap<>();
    public Debate save(Debate d) { /* ... */ }
    public Optional<Debate> findById(Long id) { return Optional.ofNullable(store.get(id)); }
}
```

이게 가능한 이유가 곧 도메인을 인프라로부터 격리한 가치다.
