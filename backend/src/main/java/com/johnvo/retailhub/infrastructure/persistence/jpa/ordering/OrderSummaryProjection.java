package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface OrderSummaryProjection {
    UUID getId();
    UUID getCustomerId();
    String getStatus();
    BigDecimal getTotalAmount();
    long getItemCount();
    Instant getCreatedAt();
    Instant getConfirmedAt();
    Instant getCancelledAt();
}
