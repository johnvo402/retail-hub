package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortParser {
    private static final Set<String> ALLOWED = Set.of("name", "sku", "price", "active", "createdAt", "updatedAt", "quantity");

    private SortParser() {
    }

    public static Sort parse(String value, String fallback) {
        String[] parts = (value == null ? "" : value).split(",", 2);
        String property = parts.length > 0 && ALLOWED.contains(parts[0]) ? parts[0] : fallback;
        Sort.Direction direction = parts.length == 2 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}

