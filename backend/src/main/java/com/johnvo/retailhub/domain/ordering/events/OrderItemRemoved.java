package com.johnvo.retailhub.domain.ordering.events;

import java.time.Instant;
import java.util.UUID;

public record OrderItemRemoved(UUID aggregateId, UUID itemId, Instant occurredAt) implements OrderEvent {
}

