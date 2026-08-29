package com.johnvo.retailhub.application.features.dashboard.common;

import java.math.BigDecimal;
import java.util.List;

public record DashboardOverviewView(
        long activeProductCount,
        long inventoryLineCount,
        long lowStockCount,
        long draftOrderCount,
        long confirmedOrderCount,
        BigDecimal confirmedOrderValue,
        List<DashboardOrderSummaryView> recentOrders,
        List<DashboardLowStockView> lowStockItems
) {
    public DashboardOverviewView {
        confirmedOrderValue = confirmedOrderValue == null ? BigDecimal.ZERO : confirmedOrderValue;
        recentOrders = List.copyOf(recentOrders);
        lowStockItems = List.copyOf(lowStockItems);
    }
}
