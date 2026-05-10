# debate-zip-backend-demo

Java 25 + Spring Boot 4.0 기반 **멀티모듈 DDD 백엔드**.
의존성 방향과 모듈 경계를 Gradle + ArchUnit으로 컴파일/빌드 시점에 강제한다.

## 기술 스택

| 영역 | 선택 |
|------|------|
| 언어 | Java 25 (Gradle toolchain이 자동 프로비저닝) |
| 프레임워크 | Spring Boot 4.0.6 |
| 빌드 | Gradle 9.4.1 (Kotlin DSL) |
| ORM / DB | Spring Data JPA + H2 (인메모리, 개발/테스트용) |
| 테스트 | JUnit 5 + Spring Boot Test + ArchUnit 1.4.2 |

## 빠른 시작

```bash
# 1. 빌드 + 모든 테스트 (ArchUnit 의존성 룰 포함)
./gradlew build

# 2. 앱 실행 (H2 인메모리, 8080 포트)
./gradlew :api:bootRun
```

> 첫 빌드는 Java 25 toolchain을 [foojay](https://foojay.io/)에서 자동 다운로드하므로 5–10분 걸린다. 이후에는 캐시된다.

## 모듈 구조

```
debate-zip-backend-demo/
├── common/   ← 공통 예외/에러 코드 (의존성 0)
├── domain/   ← 도메인 객체 + 포트 인터페이스 + use case
├── infra/    ← JPA 엔티티 + 도메인 포트 어댑터 구현
└── api/      ← Spring Boot 진입점, HTTP 어댑터, ArchUnit 룰
```

의존성 방향(단방향, 순환 없음):

```
common  ←  domain  ←  infra  ←  api
                        ↑─────────┘
```

`settings.gradle.kts`에 등록되지 않은 모듈은 어디서도 참조할 수 없다 — **The Wall**.

자세한 모듈별 가드레일은 각 모듈 루트의 `CLAUDE.md`를 참고:
- [common/CLAUDE.md](common/CLAUDE.md) — 공통 모듈에 들어와도 되는 것 / 안 되는 것
- [domain/CLAUDE.md](domain/CLAUDE.md) — 도메인 작성 패턴, JPA 금지 원칙
- [infra/CLAUDE.md](infra/CLAUDE.md) — 어댑터 변환 패턴, package-private 캡슐화
- [api/CLAUDE.md](api/CLAUDE.md) — 컨트롤러 작성 규칙, DTO 분리 원칙

## 절대 어기지 말 다섯 가지

1. **Repository는 도메인이 인터페이스로만 안다.** 컨트롤러에서 `JpaRepository`/`*RepositoryAdapter` 직접 주입 금지.
2. **`domain` 모듈에 `spring-data-jpa`, `spring-web`을 추가하지 않는다.**
3. **`common`에 비즈니스 로직을 넣지 않는다.** (모든 모듈이 의존하므로 한 줄 변경 = 전체 재빌드)
4. **순환 참조가 보이면 `@Lazy`가 아니라 `application` 모듈을 신설한다.**
5. **JPA 엔티티와 도메인 객체를 같은 클래스로 쓰지 않는다.**

위반 시 ArchUnit이 빌드를 깬다. 자세한 룰은 `api/src/test/java/com/debatezip/api/arch/ArchitectureTest.java`.

## 새 도메인 추가하기

`domain/sample/*`과 `infra/persistence/sample/*`이 살아있는 템플릿이다.
새 도메인 `Foo`를 추가하려면 세 모듈에 동일 패턴으로:

| 모듈 | 위치 | 파일 |
|------|------|------|
| `domain` | `com.debatezip.domain.foo` | `Foo`, `FooRepository` (인터페이스), `FooService` |
| `infra` | `com.debatezip.infra.persistence.foo` | `FooJpaEntity` (package-private), `FooJpaRepository` (package-private), `FooRepositoryAdapter` |
| `api` | `com.debatezip.api.foo` | `FooController`, `FooDtos` |

자세한 단계별 패턴은 [루트 CLAUDE.md](CLAUDE.md#새-도메인-추가-워크플로우) 참고.

## 자주 쓰는 명령

```bash
./gradlew build                                   # 전체 빌드 + 모든 테스트
./gradlew :api:bootRun                            # 앱 실행
./gradlew :api:test --tests ArchitectureTest      # 의존성 룰만 검사
./gradlew :domain:compileJava                     # 단일 모듈 컴파일
./gradlew clean                                   # 빌드 산출물 정리
```

## AI 페어 프로그래밍

이 프로젝트는 Claude Code 기반 페어 프로그래밍을 전제로 가드레일이 정리되어 있다.

- **Claude Code 사용자**: 루트와 모듈별 `CLAUDE.md`가 자동 로드된다. 별도 설정 불필요.
- **다른 AI 도구 (Cursor / Aider / Codex 등)**: [AGENTS.md](AGENTS.md)를 참조한다.

## 학습 자료

이 구조의 사상적 배경:

- [Hexagonal Architecture (Ports & Adapters)](https://alistair.cockburn.us/hexagonal-architecture/) — Alistair Cockburn
- [Dependency Inversion Principle](https://en.wikipedia.org/wiki/Dependency_inversion_principle)
- [ArchUnit](https://www.archunit.org/) — JVM 아키텍처 룰을 테스트로 강제
- 사내 학습 리포트(good-example, Kotlin 기반)를 Java로 포팅한 결과물이 이 프로젝트의 출발점

## 기여 가이드

1. 새 코드를 작성하기 전에 해당 모듈의 `CLAUDE.md`를 읽는다.
2. 새 도메인을 추가할 때 `sample`을 그대로 복사해 시작한다.
3. PR 전에 반드시 `./gradlew build`로 ArchUnit 통과를 확인한다.
4. ArchUnit 룰을 일시적으로 우회하지 않는다 (`allowEmptyShould(true)` 등). 룰이 잡는 건 거의 항상 진짜 설계 문제다.
