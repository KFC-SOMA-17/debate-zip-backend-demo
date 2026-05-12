package com.debatezip.infra.stt.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "debatezip.stt.azure")
public record AzureSpeechProperties(String subscriptionKey, String region) {
}
