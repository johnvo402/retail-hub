package com.johnvo.retailhub.application.features.ordering.common;

import com.johnvo.retailhub.domain.ordering.events.OrderEvent;

import java.util.List;

public interface OrderProjection {
    void apply(List<OrderEvent> events);
}

