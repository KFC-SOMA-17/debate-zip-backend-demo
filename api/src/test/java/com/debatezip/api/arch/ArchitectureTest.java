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

    // Rule 3: api는 JPA 엔티티/레포지토리에 직접 접근할 수 없다.
    // LazyInitializationException, N+1, 영속성 컨텍스트 누수가 컨트롤러까지 흘러가는 사고를 구조적으로 차단한다.
    @ArchTest
    static final ArchRule api_should_not_depend_on_jpa_persistence = noClasses()
            .that().resideInAPackage("com.debatezip.api..")
            .should().dependOnClassesThat().resideInAPackage("com.debatezip.infra.persistence..");

    // Rule 4: infra는 진입점(api)을 알지 못한다.
    // 어댑터가 진입점을 역참조하는 순간 의존성 방향이 깨지고 순환 참조의 단초가 된다.
    @ArchTest
    static final ArchRule infra_should_not_depend_on_api = noClasses()
            .that().resideInAPackage("com.debatezip.infra..")
            .should().dependOnClassesThat().resideInAPackage("com.debatezip.api..");
}
