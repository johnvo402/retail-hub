package com.johnvo.retailhub.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("retailhub.cache")
public record CacheProperties(Duration productTtl) {
}

