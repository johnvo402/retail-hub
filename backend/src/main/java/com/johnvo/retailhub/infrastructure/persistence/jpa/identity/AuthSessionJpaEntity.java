package com.johnvo.retailhub.infrastructure.persistence.jpa.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Version
    @Column(nullable = false)
    private long version;

    protected AuthSessionJpaEntity() {
    }

    public AuthSessionJpaEntity(UUID id, UserJpaEntity user, String refreshTokenHash,
                                Instant expiresAt, Instant revokedAt, Instant createdAt,
                                Instant lastUsedAt, String userAgent, long version) {
        this.id = id;
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.userAgent = userAgent;
        this.version = version;
    }

    public void update(Instant revokedAt, Instant lastUsedAt) {
        this.revokedAt = revokedAt;
        this.lastUsedAt = lastUsedAt;
    }

    public UUID getId() { return id; }
    public UserJpaEntity getUser() { return user; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public String getUserAgent() { return userAgent; }
    public long getVersion() { return version; }
}

