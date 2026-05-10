package com.debatezip.domain.sample;

import com.debatezip.common.exception.BusinessException;
import com.debatezip.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

// 단일 도메인 안에서 완결되는 use case를 담는다.
// 다른 도메인을 호출해야 하는 흐름이 생기면 application 모듈을 신설해 그곳으로 끌어올린다.
@Service
public class SampleService {

    private final SampleRepository sampleRepository;

    public SampleService(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    public Sample register(String name) {
        return sampleRepository.save(Sample.create(name));
    }

    public Sample getById(Long id) {
        return sampleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
