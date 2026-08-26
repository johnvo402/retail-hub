package com.johnvo.retailhub.application.features.catalog.common;

import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductView(
        UUID id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        UUID categoryId,
        String categoryName,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductView from(Product product, Category category) {
        return new ProductView(product.id().value(), product.name(), product.description(), product.sku(),
                product.price(), category.id().value(), category.name(), product.active(),
                product.createdAt(), product.updatedAt());
    }
}

