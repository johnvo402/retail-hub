package com.johnvo.retailhub.domain.catalog;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {
    public CategoryId {
        Objects.requireNonNull(value, "Category id is required");
    }

    public static CategoryId newId() {
        return new CategoryId(UUID.randomUUID());
    }
}

