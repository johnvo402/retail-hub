package com.johnvo.retailhub.application.features.catalog.common;

import java.util.Optional;
import java.util.UUID;

public interface ProductCache {
    Optional<ProductView> get(UUID productId);

    void put(ProductView product);

    void evict(UUID productId);
}

