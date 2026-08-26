package com.johnvo.retailhub.infrastructure.persistence.jpa.identity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAuthSessionRepository extends JpaRepository<AuthSessionJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSessionJpaEntity s join fetch s.user " +
            "where s.refreshTokenHash = :hash and s.revokedAt is null")
    Optional<AuthSessionJpaEntity> findActiveForUpdate(@Param("hash") String hash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AuthSessionJpaEntity s set s.revokedAt = :now, s.lastUsedAt = :now " +
            "where s.user.id = :userId and s.revokedAt is null")
    int revokeAllActive(@Param("userId") UUID userId, @Param("now") Instant now);
}

