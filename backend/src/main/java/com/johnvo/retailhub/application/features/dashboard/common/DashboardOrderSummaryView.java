package com.johnvo.retailhub.application.features.dashboard.common;

import com.johnvo.retailhub.domain.ordering.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DashboardOrderSummaryView(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
