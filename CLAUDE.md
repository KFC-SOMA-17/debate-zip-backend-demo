# debatezip — 프로젝트 컨텍스트

> 이 파일은 Claude Code가 작업 시 자동으로 읽는다. 팀원이 클로드와 페어할 때 일관된 결정을 내리도록 하는 가드레일이다.
> 코드를 읽으면 알 수 있는 사실은 여기 적지 않는다. **"왜 이렇게 설계되었는가"**와 **"무엇을 절대 하지 말아야 하는가"**만 적는다.

## 한 줄 요약

Java 25 + Spring Boot 4.0.6 기반 멀티모듈 DDD 백엔드. 의존성 방향과 모듈 경계를 **Gradle + ArchUnit으로 컴파일/빌드 시점에 강제**한다.

## 모듈 지도 (의존성 방향 = 단방향)

```
common  ←  domain  ←  infra  ←  api
                        ↑─────────┘  (api는 infra 외 모든 모듈 직접 의존)
```

| 모듈 | 책임 | 자세한 가드레일 |
|------|------|----------------|
| `common` | 공통 예외/에러 코드만 | [common/CLAUDE.md](common/CLAUDE.md) |
| `domain` | 도메인 객체 + 포트 인터페이스 + 단일 도메인 use case | [domain/CLAUDE.md](domain/CLAUDE.md) |
| `infra` | 도메인 포트의 어댑터 구현, JPA 엔티티 | [infra/CLAUDE.md](infra/CLAUDE.md) |
| `api` | HTTP 진입점, DI 조립, ArchUnit 룰 | [api/CLAUDE.md](api/CLAUDE.md) |

`settings.gradle.kts`에 등록되지 않은 모듈은 어디서도 참조할 수 없다 — **The Wall**.

## 절대 어기지 말 다섯 가지

1. **Repository는 도메인이 인터페이스로만 안다.** 컨트롤러나 ApplicationService에서 `JpaRepository`, `*RepositoryAdapter`를 직접 주입받으면 ArchUnit이 빌드를 깬다.
2. **`domain` 모듈에 `spring-data-jpa`, `spring-web`을 추가하지 않는다.** 도메인이 인프라 기술에 오염되는 순간 DIP가 깨진다.
3. **`common`에 비즈니스 로직을 넣지 않는다.** 모든 모듈이 의존하므로 `common` 한 줄 변경 = 전체 재빌드 + 도메인 정책 결합 위험.
4. **순환 참조가 보이면 `@Lazy`가 아니라 `application` 모듈을 신설한다.** 두 도메인을 모두 알아야 하는 use case는 한 단계 위로 끌어올린다 (현재 4모듈 MVP에는 없음, 두 번째 도메인이 첫 번째를 호출하기 시작하면 추가).
5. **JPA 엔티티와 도메인 객체를 같은 클래스로 쓰지 않는다.** `Sample`(domain) ↔ `SampleJpaEntity`(infra)는 항상 다른 클래스이며 어댑터에서 변환한다.

## 자주 쓰는 명령

| 작업 | 명령 |
|------|------|
| 전체 빌드 + 모든 테스트 (ArchUnit 포함) | `./gradlew build` |
| 앱 실행 (H2 인메모리) | `./gradlew :api:bootRun` |
| 의존성 룰 검사만 | `./gradlew :api:test --tests ArchitectureTest` |
| 단일 모듈 컴파일 | `./gradlew :domain:compileJava` |

처음 빌드는 Java 25 toolchain을 foojay에서 자동 다운로드하므로 시간이 걸린다 (5~10분).

## 새 도메인 추가 워크플로우

`domain/sample/*`과 `infra/persistence/sample/*`이 살아있는 템플릿이다. 새 도메인 `Foo` 추가 시:

1. `domain/src/main/java/com/debatezip/domain/foo/`
   - `Foo.java` — 순수 record/클래스. `@Entity` 금지.
   - `FooRepository.java` — 인터페이스(Port). 구현은 다음 단계에서.
   - `FooService.java` — `@Service`. 단일 도메인 안에서 끝나는 use case만.
2. `infra/src/main/java/com/debatezip/infra/persistence/foo/`
   - `FooJpaEntity.java` — `@Entity` (package-private 권장).
   - `FooJpaRepository.java` — `JpaRepository<FooJpaEntity, Long>` (package-private).
   - `FooRepositoryAdapter.java` — `@Repository`, `FooRepository` 구현, 도메인↔JPA 변환.
3. `api/src/main/java/com/debatezip/api/foo/`
   - `FooController.java` — `FooService`만 주입. Repository 주입은 ArchUnit이 막는다.
   - `FooDtos.java` — 요청/응답 DTO. 도메인 객체와 별개.

`Foo`가 다른 도메인을 호출해야 하면 → 이 흐름을 `domain/foo/FooService`가 아니라 새로 만들 `application` 모듈로 옮긴다.

## ArchUnit 룰이 빌드를 깰 때

위반 메시지는 보통 친절하다. 룰별 대응:

- **"... should depend on classes that have simple name ending with 'Repository'"** → 컨트롤러나 ExceptionHandler에서 Repository를 직접 주입했다. `Service`를 통해 호출하도록 수정.
- **"domain... should not depend on api/infra"** → 도메인에서 `@RestController`, `JpaRepository` 같은 걸 import했다. 인터페이스만 도메인에 남기고 구현을 `infra`로.
- **"api... should not depend on infra.persistence"** → 컨트롤러에서 `*JpaEntity`나 `*JpaRepository`를 import했다. 도메인 인터페이스 타입으로 받도록 수정.
- **"infra... should not depend on api"** → 어댑터가 컨트롤러나 DTO를 import했다. 인프라가 진입점을 알면 안 된다 — 변환 책임 위치를 다시 점검.

룰 위반을 일시적으로 우회하지 말 것 (`@SuppressWarnings`나 `allowEmptyShould`로 룰을 무력화하지 않는다). 룰이 잡는 건 거의 항상 진짜 설계 문제다.

## DB / 인프라 결정사항

- **개발/테스트 DB**: H2 인메모리 (`MODE=MySQL`). 운영 DB로 바꾸려면 `infra/build.gradle.kts`의 `runtimeOnly`만 교체. 도메인 코드는 한 줄도 안 변한다 — 그게 이 구조의 핵심.
- **DDL**: `create-drop`. 운영 전환 시 Flyway/Liquibase로 마이그레이션 도입 필요.

## 빌드/툴체인 메모

- Java 25는 시스템에 없으면 foojay가 자동 프로비저닝 (`settings.gradle.kts`).
- ArchUnit은 1.4.2 이상이어야 Java 25 클래스 파일(major 69)을 읽을 수 있다. 다운그레이드하지 말 것.
- `bootJar`는 `api` 모듈에서만 활성화된다. 라이브러리 모듈(common/domain/infra)은 일반 jar.

## 학습 자료

이 구조의 사상적 배경:
- Hexagonal Architecture (Ports & Adapters) — Alistair Cockburn
- Dependency Inversion Principle
- 사내 학습 리포트(good-example, Kotlin 기반)를 Java로 포팅한 것이 이 프로젝트의 출발점
