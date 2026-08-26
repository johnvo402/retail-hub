package com.johnvo.retailhub.domain.ordering.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemAdded(
        UUID aggregateId,
        UUID itemId,
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        Instant occurredAt
) implements OrderEvent {
}

