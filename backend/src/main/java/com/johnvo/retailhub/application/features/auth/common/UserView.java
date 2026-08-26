package com.johnvo.retailhub.application.features.auth.common;

import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRole;

import java.util.UUID;

public record UserView(UUID id, String email, UserRole role) {
    public static UserView from(User user) {
        return new UserView(user.id(), user.email(), user.role());
    }
}

