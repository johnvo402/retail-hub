package com.johnvo.retailhub.application.features.inventory.common;

import java.time.Instant;
import java.util.UUID;

public record InventoryView(
        UUID productId,
        String sku,
        String productName,
        int quantity,
        long version,
        Instant updatedAt
) {
}

