package com.johnvo.retailhub.domain.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilter(
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean active,
        String keyword,
        int page,
        int size,
        String sort
) {
    public ProductFilter {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        sort = sort == null || sort.isBlank() ? "createdAt,desc" : sort;
    }
}

