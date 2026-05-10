package com.debatezip.domain.sample;

import java.util.Optional;

// 도메인이 필요로 하는 영속성 동작을 인터페이스로 선언한다 (Port).
// 구현체(Adapter)는 infra 모듈이 책임지며, 도메인은 어떤 DB/ORM이 쓰이는지 알지 못한다.
public interface SampleRepository {

    Sample save(Sample sample);

    Optional<Sample> findById(Long id);
}
