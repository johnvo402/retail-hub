package com.johnvo.retailhub.api.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
