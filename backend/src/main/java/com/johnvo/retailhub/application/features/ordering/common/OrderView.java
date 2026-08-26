package com.johnvo.retailhub.application.features.ordering.common;

import com.johnvo.retailhub.domain.ordering.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderView(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        int itemCount,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        List<OrderItemView> items
) {
    public OrderView {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

