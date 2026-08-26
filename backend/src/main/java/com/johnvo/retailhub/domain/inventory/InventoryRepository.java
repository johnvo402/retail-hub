package com.johnvo.retailhub.domain.inventory;

import com.johnvo.retailhub.domain.catalog.ProductId;

import java.util.Optional;

public interface InventoryRepository {
    Optional<InventoryItem> findByProductId(ProductId productId);

    InventoryItem save(InventoryItem item);
}

