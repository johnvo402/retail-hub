package com.johnvo.retailhub.application.features.inventory.common;

import java.util.UUID;

public record StockAdjustmentResult(UUID productId, int quantity, long version) {
}

