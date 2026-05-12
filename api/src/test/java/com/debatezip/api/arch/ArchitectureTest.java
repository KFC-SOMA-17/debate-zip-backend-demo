package com.debatezip.api.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// 모듈 간 의존성 규칙을 빌드 시점에 강제한다.
@AnalyzeClasses(packages = "com.debatezip", importOptions = {})
class ArchitectureTest {

    // Rule 1: 컨트롤러는 Repository를 직접 의존할 수 없다.
    // 반드시 Service / ApplicationService를 통해서만 데이터에 접근한다.
    @ArchTest
    static final ArchRule api_should_not_depend_on_repositories = noClasses()
            .that().resideInAPackage("com.debatezip.api..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    // Rule 2: 도메인은 api/infra를 알지 못한다.
    // 도메인이 인프라 기술에 오염되지 않도록 의존 방향을 단방향으로 강제한다.
    @ArchTest
    static final ArchRule domain_should_not_depend_on_api_or_infra = noClasses()
            .that().resideInAPackage("com.debatezip.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.debatezip.api..", "com.debatezip.infra..");

    // Rule 3: api는 infra 모듈의 내부 구현에 직접 의존할 수 없다.
    // 모든 외부 시스템(JPA, STT, OAuth 등) 어댑터는 도메인 포트 인터페이스로만 주입받는다.
    //   - JPA: LazyInitializationException, N+1, 영속성 컨텍스트 누수 차단
    //   - STT/OAuth: 외부 SDK 타입이 컨트롤러까지 흘러가는 사고 차단
    @ArchTest
    static final ArchRule api_should_not_depend_on_infra_internals = noClasses()
            .that().resideInAPackage("com.debatezip.api..")
            .should().dependOnClassesThat().resideInAPackage("com.debatezip.infra..");

    // Rule 4: infra는 진입점(api)을 알지 못한다.
    // 어댑터가 진입점을 역참조하는 순간 의존성 방향이 깨지고 순환 참조의 단초가 된다.
    @ArchTest
    static final ArchRule infra_should_not_depend_on_api = noClasses()
            .that().resideInAPackage("com.debatezip.infra..")
            .should().dependOnClassesThat().resideInAPackage("com.debatezip.api..");

    // Rule 5: infra 모듈끼리는 서로 알지 못한다.
    // STT 어댑터가 persistence 어댑터를, 또는 그 반대를 직접 import하면 모듈 격리가 무너진다.
    // 두 인프라가 협력해야 한다면 그 흐름은 도메인/application 레이어에 두어야 한다.
    @ArchTest
    static final ArchRule persistence_should_not_depend_on_stt = noClasses()
            .that().resideInAPackage("com.debatezip.infra.persistence..")
            .should().dependOnClassesThat().resideInAPackage("com.debatezip.infra.stt..");

    @ArchTest
    static final ArchRule stt_should_not_depend_on_persistence = noClasses()
            .that().resideInAPackage("com.debatezip.infra.stt..")
            .should().dependOnClassesThat().resideInAPackage("com.debatezip.infra.persistence..");
}
