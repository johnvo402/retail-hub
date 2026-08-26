package com.johnvo.retailhub.domain.ordering.events;

import java.time.Instant;
import java.util.UUID;

public record OrderCreated(UUID aggregateId, UUID customerId, Instant occurredAt) implements OrderEvent {
}

