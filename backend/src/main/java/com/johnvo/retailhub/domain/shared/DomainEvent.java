package com.johnvo.retailhub.domain.shared;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID aggregateId();

    Instant occurredAt();
}

