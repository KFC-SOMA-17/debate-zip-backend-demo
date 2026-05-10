plugins {
    // 시스템에 설치되지 않은 Java toolchain을 자동으로 프로비저닝한다.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "debatezip"

// 모듈 목록 = 이 프로젝트의 의존성 지도
// 여기 없는 모듈은 서로 참조 자체가 불가능하다 (The Wall)
include(
    "common",   // 공통 예외/에러 코드 — 최소한으로 유지
    "domain",   // 도메인 객체 + 포트 인터페이스 (인프라 기술 미포함)
    "infra",    // JPA 엔티티 + 도메인 포트 어댑터 구현
    "api"       // Spring Boot 진입점, HTTP 어댑터, DI 조립
)
