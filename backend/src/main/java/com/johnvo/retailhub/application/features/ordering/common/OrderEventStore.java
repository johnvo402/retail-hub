package com.johnvo.retailhub.application.features.ordering.common;

import com.johnvo.retailhub.domain.ordering.events.OrderEvent;

import java.util.List;
import java.util.UUID;

public interface OrderEventStore {
    List<OrderEvent> load(UUID orderId);

    void append(UUID orderId, long expectedVersion, List<OrderEvent> events);
}

