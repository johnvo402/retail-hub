package com.johnvo.retailhub.application.features.catalog.common;

import com.johnvo.retailhub.domain.catalog.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryView(
        UUID id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryView from(Category category) {
        return new CategoryView(category.id().value(), category.name(), category.description(),
                category.active(), category.createdAt(), category.updatedAt());
    }
}

