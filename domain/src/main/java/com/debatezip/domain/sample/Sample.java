package com.debatezip.domain.sample;

// 도메인 객체는 순수 Java로 둔다. JPA 어노테이션(@Entity 등) 금지.
// 영속성 매핑은 infra 모듈의 JPA 엔티티가 담당하고, 어댑터에서 변환한다.
//
// 이 자리에 실제 도메인(예: Debate, Argument, Vote 등)을 추가하라.
public record Sample(Long id, String name) {

    public static Sample create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new Sample(null, name);
    }
}
