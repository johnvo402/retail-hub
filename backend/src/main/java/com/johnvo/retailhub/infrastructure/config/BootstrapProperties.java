package com.johnvo.retailhub.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("retailhub.bootstrap")
public record BootstrapProperties(String adminEmail, String adminPassword) {
}

