package com.johnvo.retailhub.application.common.security;

import com.johnvo.retailhub.domain.identity.UserRole;

import java.util.UUID;

public interface AccessTokenVerifier {
    AuthenticatedUser verify(String token);

    record AuthenticatedUser(UUID id, UserRole role) {
    }
}

