# infra 모듈 — 가드레일

> 도메인이 인터페이스로 선언한 협력자(Port)의 실제 구현(Adapter)이 사는 곳.
> JPA 엔티티/레포지토리도 이 모듈에 함께 둔다 (4모듈 MVP 구성).

## 책임

```
com.debatezip.infra/
├── persistence/<도메인>/         ← JPA 어댑터들
│   ├── <도메인>JpaEntity.java    ← @Entity (package-private)
│   ├── <도메인>JpaRepository.java ← Spring Data (package-private)
│   └── <도메인>RepositoryAdapter.java ← @Repository, public, 도메인 포트 구현
├── notification/                 ← 알림 채널 어댑터 (예: EmailNotificationAdapter)
└── security/                     ← 암호화/인증 어댑터 (예: BCryptPasswordEncoderAdapter)
```

## 핵심 원칙: Java에는 internal이 없다

Kotlin이라면 `*JpaEntity`, `*JpaRepository`를 `internal`로 막을 수 있지만, Java에는 그게 없다. 대신 두 가지로 막는다:

1. **package-private 가시성** — 클래스에 `public`을 붙이지 않는다. 같은 패키지 안의 어댑터만 접근 가능.
2. **ArchUnit 룰 `api_should_not_depend_on_jpa_persistence`** — 만에 하나 누군가 `public`으로 풀어도 `api` 모듈에서 import하면 빌드가 깨진다.

따라서:
- `*JpaEntity` → **package-private** (`class FooJpaEntity` ← `public` 금지)
- `*JpaRepository` → **package-private** (`interface FooJpaRepository` ← `public` 금지)
- `*RepositoryAdapter` → **public** (Spring DI가 다른 모듈에서 주입할 수 있어야 함)

`SampleJpaEntity.java`, `SampleJpaRepository.java`가 이 패턴을 따르고 있으니 참고.

## 의존성

```kotlin
dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
}
```

운영 DB로 바꾸려면 `runtimeOnly`만 교체 (예: `runtimeOnly("org.postgresql:postgresql")`). 도메인 코드는 변경되지 않는다.

## 어댑터 작성 패턴

도메인 ↔ JPA 엔티티 변환은 **오직 어댑터 안에서만** 일어난다. 변환 로직이 컨트롤러나 서비스로 새어나가지 않게 한다.

```java
@Repository
public class DebateRepositoryAdapter implements DebateRepository {
    private final DebateJpaRepository jpaRepository;

    public DebateRepositoryAdapter(DebateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Debate save(Debate debate) {
        return jpaRepository.save(DebateJpaEntity.from(debate)).toDomain();
    }

    @Override
    public Optional<Debate> findById(Long id) {
        return jpaRepository.findById(id).map(DebateJpaEntity::toDomain);
    }
}
```

`from()`, `toDomain()`은 JPA 엔티티 자체에 두는 게 깔끔하다 (`SampleJpaEntity` 참고).

## 들어와도 되는 것

- JPA 엔티티 (`@Entity`, `@Table`)
- `JpaRepository` 상속 인터페이스
- 도메인 포트의 어댑터 구현 (`@Repository`, `@Component`)
- 외부 시스템 클라이언트 어댑터 (Email, Slack, S3, 외부 API)
- DB 마이그레이션 스크립트 (Flyway/Liquibase 도입 시)

## 절대 안 되는 것

- 비즈니스 로직 — 도메인 객체 메서드 / 도메인 서비스로
- `api` 모듈 import (`com.debatezip.api.*`) — ArchUnit이 막는다
- 도메인 객체에 `@Entity` 직접 붙이기 — 별개 클래스로 분리
- JPA 엔티티를 어댑터 밖으로 노출 (반환 타입에 사용 금지)

## 트랜잭션 위치

- `@Transactional`은 보통 도메인 서비스(`*Service`) 또는 `application` 모듈의 ApplicationService에 둔다.
- 어댑터에는 두지 않는다 (어댑터는 단일 호출 단위로 동작).

## 새 외부 시스템 어댑터 추가 시

1. 도메인이 필요로 하는 동작을 `domain/<도메인>/*Port.java`에 인터페이스로 선언
2. `infra/<카테고리>/`에 어댑터 구현 (`@Component`)
3. `domain` 모듈 코드는 인터페이스만 안다 — 구현체가 SMTP인지 SES인지 SendGrid인지 모른다
