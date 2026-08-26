package com.johnvo.retailhub.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("retailhub.security")
public record SecurityProperties(
        String jwtSecret,
        long accessExpirationSeconds,
        long refreshExpirationSeconds,
        boolean cookieSecure,
        String cookieSameSite,
        String cookieDomain,
        String corsAllowedOrigin
) {
}

