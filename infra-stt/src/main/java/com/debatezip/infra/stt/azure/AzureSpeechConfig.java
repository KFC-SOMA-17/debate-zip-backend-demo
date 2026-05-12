package com.debatezip.infra.stt.azure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// AzureSpeechProperties를 Spring 컨텍스트에 등록한다.
// package-private — 다른 모듈은 이 클래스를 알 필요가 없다.
@Configuration
@EnableConfigurationProperties(AzureSpeechProperties.class)
class AzureSpeechConfig {
}
