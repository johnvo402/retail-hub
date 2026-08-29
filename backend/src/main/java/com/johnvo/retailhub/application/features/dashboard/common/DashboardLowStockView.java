package com.johnvo.retailhub.application.features.dashboard.common;

import java.util.UUID;

public record DashboardLowStockView(
        UUID productId,
        String productName,
        String sku,
        int quantity
) {
}
