package com.johnvo.retailhub.domain.identity;

import com.johnvo.retailhub.domain.shared.DomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthSession {
    private final UUID id;
    private final UUID userId;
    private final String refreshTokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;
    private final Instant createdAt;
    private Instant lastUsedAt;
    private final String userAgent;
    private long version;

    private AuthSession(UUID id, UUID userId, String refreshTokenHash, Instant expiresAt,
                        Instant revokedAt, Instant createdAt, Instant lastUsedAt,
                        String userAgent, long version) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revokedAt = revokedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.lastUsedAt = Objects.requireNonNull(lastUsedAt);
        this.userAgent = userAgent;
        this.version = version;
    }

    public static AuthSession create(UUID userId, String tokenHash, Instant expiresAt,
                                     String userAgent, Instant now) {
        return new AuthSession(UUID.randomUUID(), userId, tokenHash, expiresAt, null,
                now, now, userAgent, 0);
    }

    public static AuthSession reconstitute(UUID id, UUID userId, String tokenHash,
                                           Instant expiresAt, Instant revokedAt,
                                           Instant createdAt, Instant lastUsedAt,
                                           String userAgent, long version) {
        return new AuthSession(id, userId, tokenHash, expiresAt, revokedAt,
                createdAt, lastUsedAt, userAgent, version);
    }

    public void assertUsable(Instant now) {
        if (revokedAt != null) {
            throw new DomainException("Refresh session has been revoked");
        }
        if (!expiresAt.isAfter(now)) {
            throw new DomainException("Refresh session has expired");
        }
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
        lastUsedAt = now;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String refreshTokenHash() { return refreshTokenHash; }
    public Instant expiresAt() { return expiresAt; }
    public Instant revokedAt() { return revokedAt; }
    public Instant createdAt() { return createdAt; }
    public Instant lastUsedAt() { return lastUsedAt; }
    public String userAgent() { return userAgent; }
    public long version() { return version; }
}

