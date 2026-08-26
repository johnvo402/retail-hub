package com.johnvo.retailhub.application.common.security;

import com.johnvo.retailhub.domain.identity.User;

import java.time.Instant;

public interface TokenService {
    AccessToken issueAccessToken(User user);

    RefreshToken issueRefreshToken(Instant now);

    String hashRefreshToken(String rawToken);

    record AccessToken(String value, long expiresInSeconds) {
    }

    record RefreshToken(String value, String hash, Instant expiresAt) {
    }
}

