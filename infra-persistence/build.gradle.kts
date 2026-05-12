dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // 인메모리 H2 — 개발/테스트 편의를 위한 기본 드라이버
    runtimeOnly("com.h2database:h2")
}
