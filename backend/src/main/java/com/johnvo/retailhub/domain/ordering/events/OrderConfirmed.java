package com.johnvo.retailhub.domain.ordering.events;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmed(UUID aggregateId, Instant occurredAt) implements OrderEvent {
}

