package com.johnvo.retailhub.application.common.security;

import com.johnvo.retailhub.domain.identity.UserRole;

import java.util.UUID;
import java.util.Optional;

public interface AccessTokenVerifier {
    Optional<AuthenticatedUser> verify(String token);

    record AuthenticatedUser(UUID id, UserRole role) {
    }
}
