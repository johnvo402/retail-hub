package com.johnvo.retailhub.domain.ordering.events;

import com.johnvo.retailhub.domain.shared.DomainEvent;

public sealed interface OrderEvent extends DomainEvent permits OrderCreated, OrderItemAdded,
        OrderItemRemoved, OrderConfirmed, OrderCancelled {
}

