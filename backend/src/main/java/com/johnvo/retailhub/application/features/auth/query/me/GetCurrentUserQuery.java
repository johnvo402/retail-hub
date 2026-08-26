package com.johnvo.retailhub.application.features.auth.query.me;

import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.auth.common.UserView;

import java.util.UUID;

public record GetCurrentUserQuery(UUID userId) implements Query<UserView> {
}

