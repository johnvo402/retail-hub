package com.johnvo.retailhub.domain.catalog;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {
    public ProductId {
        Objects.requireNonNull(value, "Product id is required");
    }

    public static ProductId newId() {
        return new ProductId(UUID.randomUUID());
    }
}

