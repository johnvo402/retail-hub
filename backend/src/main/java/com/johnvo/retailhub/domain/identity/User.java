package com.johnvo.retailhub.domain.identity;

import com.johnvo.retailhub.domain.shared.DomainException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public User {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new DomainException("A valid email address is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("Password hash is required");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
    }

    public static User register(String email, String passwordHash, UserRole role, Instant now) {
        return new User(UUID.randomUUID(), email, passwordHash, role, true, now, now);
    }
}

