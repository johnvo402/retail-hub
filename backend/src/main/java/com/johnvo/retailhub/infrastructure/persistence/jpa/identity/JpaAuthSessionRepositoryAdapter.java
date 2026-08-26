package com.johnvo.retailhub.infrastructure.persistence.jpa.identity;

import com.johnvo.retailhub.domain.identity.AuthSession;
import com.johnvo.retailhub.domain.identity.AuthSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuthSessionRepositoryAdapter implements AuthSessionRepository {
    private final SpringDataAuthSessionRepository sessions;
    private final SpringDataUserRepository users;

    public JpaAuthSessionRepositoryAdapter(SpringDataAuthSessionRepository sessions,
                                           SpringDataUserRepository users) {
        this.sessions = sessions;
        this.users = users;
    }

    @Override
    public Optional<AuthSession> findActiveByTokenHashForUpdate(String tokenHash) {
        return sessions.findActiveForUpdate(tokenHash).map(JpaAuthSessionRepositoryAdapter::toDomain);
    }

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionJpaEntity entity = sessions.findById(session.id()).orElseGet(() ->
                new AuthSessionJpaEntity(session.id(), users.getReferenceById(session.userId()),
                        session.refreshTokenHash(), session.expiresAt(), session.revokedAt(),
                        session.createdAt(), session.lastUsedAt(), session.userAgent(), session.version()));
        entity.update(session.revokedAt(), session.lastUsedAt());
        return toDomain(sessions.save(entity));
    }

    @Override
    public int revokeAllActiveByUserId(UUID userId, Instant revokedAt) {
        return sessions.revokeAllActive(userId, revokedAt);
    }

    private static AuthSession toDomain(AuthSessionJpaEntity entity) {
        return AuthSession.reconstitute(entity.getId(), entity.getUser().getId(),
                entity.getRefreshTokenHash(), entity.getExpiresAt(), entity.getRevokedAt(),
                entity.getCreatedAt(), entity.getLastUsedAt(), entity.getUserAgent(), entity.getVersion());
    }
}

