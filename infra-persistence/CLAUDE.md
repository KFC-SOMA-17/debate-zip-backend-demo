# infra-persistence 모듈 — 가드레일

> 도메인이 인터페이스로 선언한 영속성 포트(`*Repository`)의 어댑터 구현이 사는 곳.
> **JPA/RDB 한정.** STT, OAuth, S3 같은 다른 외부 시스템은 각자의 `infra-*` 모듈에 둔다.

## 책임

```
com.debatezip.infra.persistence/
└── <도메인>/
    ├── <도메인>JpaEntity.java     ← @Entity (package-private)
    ├── <도메인>JpaRepository.java ← Spring Data (package-private)
    └── <도메인>RepositoryAdapter.java ← @Repository, public, 도메인 포트 구현
```

`com.debatezip.infra.persistence` 외 다른 패키지(예: `notification`, `security`)를 이 모듈에 만들지 않는다. 알림/암호화 어댑터가 필요하면 `infra-notification`, `infra-security` 같은 별도 모듈로 신설한다 — 이 모듈은 **persistence 책임만** 진다.

## 핵심 원칙: Java에는 internal이 없다

Kotlin이라면 `*JpaEntity`, `*JpaRepository`를 `internal`로 막을 수 있지만, Java에는 그게 없다. 대신 두 가지로 막는다:

1. **package-private 가시성** — 클래스에 `public`을 붙이지 않는다. 같은 패키지 안의 어댑터만 접근 가능.
2. **ArchUnit 룰 `api_should_not_depend_on_infra_internals`** — 만에 하나 누군가 `public`으로 풀어도 `api` 모듈에서 import하면 빌드가 깨진다.

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

다른 `infra-*` 모듈(`infra-stt` 등)에 의존하지 않는다. ArchUnit 룰 `persistence_should_not_depend_on_stt`가 이를 강제한다.

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
- 도메인 영속성 포트의 어댑터 구현 (`@Repository`)
- DB 마이그레이션 스크립트 (Flyway/Liquibase 도입 시)

## 절대 안 되는 것

- 비즈니스 로직 — 도메인 객체 메서드 / 도메인 서비스로
- `api` 모듈 import — ArchUnit이 막는다
- 다른 `infra-*` 모듈 import — ArchUnit이 막는다
- 도메인 객체에 `@Entity` 직접 붙이기 — 별개 클래스로 분리
- JPA 엔티티를 어댑터 밖으로 노출 (반환 타입에 사용 금지)
- STT/OAuth/S3 등 비영속성 어댑터를 이 모듈에 추가 — 각자의 `infra-*` 모듈로

## 트랜잭션 위치

- `@Transactional`은 보통 도메인 서비스(`*Service`) 또는 `application` 모듈의 ApplicationService에 둔다.
- 어댑터에는 두지 않는다 (어댑터는 단일 호출 단위로 동작).
