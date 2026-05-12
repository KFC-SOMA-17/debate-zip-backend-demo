package com.debatezip.infra.persistence.sample;

import com.debatezip.domain.sample.Sample;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// JPA 엔티티는 도메인 객체와 별개 클래스로 둔다.
// LazyInitializationException, N+1, 영속성 컨텍스트 누수 등이 컨트롤러까지 흘러가지 않게 막는 1차 방어선이다.
//
// 외부 모듈(api 등)이 이 클래스를 직접 import 하지 못하도록 ArchUnit 규칙으로 차단한다.
@Entity
@Table(name = "sample")
class SampleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected SampleJpaEntity() {
    }

    SampleJpaEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    static SampleJpaEntity from(Sample sample) {
        return new SampleJpaEntity(sample.id(), sample.name());
    }

    Sample toDomain() {
        return new Sample(id, name);
    }

    Long getId() {
        return id;
    }
}
