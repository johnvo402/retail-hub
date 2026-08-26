package com.johnvo.retailhub.domain.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository {
    Optional<AuthSession> findActiveByTokenHashForUpdate(String tokenHash);

    AuthSession save(AuthSession session);

    int revokeAllActiveByUserId(UUID userId, Instant revokedAt);
}

