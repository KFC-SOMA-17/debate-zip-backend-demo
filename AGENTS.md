# AGENTS.md

> 이 파일은 AI 코딩 에이전트(Cursor / Aider / Codex / Claude Code 등)가 작업 시 따라야 할 가드레일을 정의한다.
> Claude Code는 `CLAUDE.md`를 자동 로드하므로 동일한 내용을 양쪽에 두지 않고, 여기서 핵심 룰을 요약하고 모듈별 상세는 `CLAUDE.md`로 위임한다.

## 프로젝트 한 줄

Java 25 + Spring Boot 4.0 기반 멀티모듈 DDD 백엔드. 의존성 방향과 모듈 경계를 **Gradle + ArchUnit으로 컴파일/빌드 시점에 강제**한다.

## 모듈 지도

```
common  ←  domain  ←  infra  ←  api
                        ↑─────────┘
```

| 모듈 | 책임 | 상세 가드레일 |
|------|------|--------------|
| `common` | 공통 예외/에러 코드 | [common/CLAUDE.md](common/CLAUDE.md) |
| `domain` | 도메인 객체 + 포트 인터페이스 + use case | [domain/CLAUDE.md](domain/CLAUDE.md) |
| `infra` | JPA 엔티티 + 도메인 포트 어댑터 구현 | [infra/CLAUDE.md](infra/CLAUDE.md) |
| `api` | HTTP 진입점, DI 조립, ArchUnit 룰 | [api/CLAUDE.md](api/CLAUDE.md) |

작업 위치에 따라 해당 모듈의 `CLAUDE.md`도 함께 읽고 따른다.

## 절대 어기지 말 다섯 가지 규칙

1. **Repository는 도메인이 인터페이스로만 안다.** 컨트롤러나 ApplicationService에서 `JpaRepository`/`*RepositoryAdapter`를 직접 주입받지 않는다.
2. **`domain` 모듈에 `spring-data-jpa`/`spring-web`을 추가하지 않는다.** 도메인이 인프라 기술에 오염되는 순간 DIP가 깨진다.
3. **`common`에 비즈니스 로직을 넣지 않는다.** 모든 모듈이 의존하므로 한 줄 변경 = 전체 재빌드 + 도메인 정책 결합 위험.
4. **순환 참조가 보이면 `@Lazy`가 아니라 `application` 모듈을 신설한다.** 두 도메인을 모두 알아야 하는 use case는 한 단계 위로 끌어올린다.
5. **JPA 엔티티와 도메인 객체를 같은 클래스로 쓰지 않는다.** 도메인 객체와 `*JpaEntity`는 항상 별개이며 어댑터에서 변환한다.

위 규칙은 `api/src/test/java/com/debatezip/api/arch/ArchitectureTest.java`의 ArchUnit 룰로 강제된다. 위반 시 빌드가 깨진다.

## 룰을 우회하지 말 것

- `allowEmptyShould(true)`로 ArchUnit 룰을 통과시키지 않는다.
- 새 룰이 잘못되었다고 판단되면 룰 자체를 수정하고 PR에 근거를 남긴다 — 일시 우회는 금지.
- ArchUnit이 잡는 건 거의 항상 진짜 설계 문제다.

## 빌드/실행

```bash
./gradlew build                                  # 전체 빌드 + ArchUnit 검사
./gradlew :api:bootRun                           # 앱 실행
./gradlew :api:test --tests ArchitectureTest     # 의존성 룰만 검증
```

## 새 도메인을 추가할 때

`domain/sample/*`과 `infra/persistence/sample/*`이 살아있는 템플릿이다. 새 도메인 `Foo`는 다음 세 모듈에 동일한 패턴으로 만든다:

```
domain/  com.debatezip.domain.foo/      Foo, FooRepository(인터페이스), FooService
infra/   com.debatezip.infra.persistence.foo/   FooJpaEntity(pkg-private), FooJpaRepository(pkg-private), FooRepositoryAdapter
api/     com.debatezip.api.foo/                  FooController, FooDtos
```

세부 단계는 [루트 CLAUDE.md](CLAUDE.md#새-도메인-추가-워크플로우) 참고.

## Java에 internal이 없다는 점에 대해

Kotlin의 `internal`(모듈-프라이빗) 같은 가시성 한정자가 Java에는 없다. 이 프로젝트는 두 겹으로 캡슐화한다:

1. **package-private 가시성** — `*JpaEntity`, `*JpaRepository`에 `public`을 붙이지 않는다.
2. **ArchUnit 룰** — 누군가 `public`으로 풀어도 `api → infra.persistence` 의존이 빌드 단계에서 차단된다.

따라서 `*JpaEntity`나 `*JpaRepository`를 `public`으로 바꾸지 말 것. 어댑터(`*RepositoryAdapter`)만 `public`이다.

## 코드 스타일 / 컨벤션

- Java 25 신기능 적극 사용 OK (record, sealed, pattern matching, switch expression).
- 도메인 객체는 record를 기본으로 한다 (불변).
- DTO도 record (요청/응답 모두).
- 의미 있는 검증은 도메인 객체의 factory 메서드(`Sample.create(...)`)에 둔다.
- `BusinessException(ErrorCode.X)`로 도메인 예외를 던진다 — `RuntimeException`을 직접 던지지 않는다.

## 작업 워크플로우

1. 작업 시작 전: 영향 받는 모듈의 `CLAUDE.md`를 읽는다.
2. 코드 변경 후: `./gradlew build`로 ArchUnit 통과 확인.
3. 새 의존성 추가 시: 해당 모듈의 `CLAUDE.md`에서 "들어와도 되는 것 / 안 되는 것" 검증.
4. 빌드가 깨지면: 룰을 우회하지 말고 설계를 재검토. ArchUnit 메시지가 친절하다.

## 학습 자료

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) — Alistair Cockburn
- [DIP](https://en.wikipedia.org/wiki/Dependency_inversion_principle)
- [ArchUnit](https://www.archunit.org/)
