package com.johnvo.retailhub.application.features.auth.common;

import java.time.Instant;

public record AuthenticationResult(
        String accessToken,
        long expiresIn,
        UserView user,
        String refreshToken,
        Instant refreshExpiresAt
) {
}

