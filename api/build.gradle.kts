// api 모듈 — Spring Boot 진입점.
// HTTP 어댑터 역할만 담당하며, 모든 모듈을 implementation으로 끌어와 DI 조립을 수행한다.
// 비즈니스 로직을 직접 작성하지 않는다 — 작성하려는 충동이 들면 도메인/application으로 옮겨야 한다는 신호.

plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2") // 의존성 규칙을 빌드에 강제하기 위한 ArchUnit
}
