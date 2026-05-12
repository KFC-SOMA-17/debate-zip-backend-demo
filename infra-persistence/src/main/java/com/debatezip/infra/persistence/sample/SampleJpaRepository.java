package com.debatezip.infra.persistence.sample;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data JPA 레포지토리는 infra 모듈 안에서만 사용한다.
// 외부 모듈에서 이 인터페이스를 주입받지 못하도록 package-private 접근 제한 + ArchUnit 차단.
interface SampleJpaRepository extends JpaRepository<SampleJpaEntity, Long> {
}
