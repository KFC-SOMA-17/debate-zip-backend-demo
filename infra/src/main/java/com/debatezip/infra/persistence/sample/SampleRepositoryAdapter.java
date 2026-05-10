package com.debatezip.infra.persistence.sample;

import com.debatezip.domain.sample.Sample;
import com.debatezip.domain.sample.SampleRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

// 도메인이 선언한 포트(SampleRepository)의 어댑터 구현.
// 도메인 객체 ↔ JPA 엔티티 변환은 오직 이 클래스 안에서만 일어난다.
//
// 외부 모듈은 SampleRepository 인터페이스 타입으로만 이 어댑터를 주입받는다.
@Repository
public class SampleRepositoryAdapter implements SampleRepository {

    private final SampleJpaRepository jpaRepository;

    public SampleRepositoryAdapter(SampleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Sample save(Sample sample) {
        SampleJpaEntity saved = jpaRepository.save(SampleJpaEntity.from(sample));
        return saved.toDomain();
    }

    @Override
    public Optional<Sample> findById(Long id) {
        return jpaRepository.findById(id).map(SampleJpaEntity::toDomain);
    }
}
