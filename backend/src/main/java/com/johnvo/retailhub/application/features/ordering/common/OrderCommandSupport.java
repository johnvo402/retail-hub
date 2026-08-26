package com.johnvo.retailhub.application.features.ordering.common;

import com.johnvo.retailhub.application.common.ForbiddenException;
import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.events.OrderEvent;
import com.johnvo.retailhub.domain.shared.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OrderCommandSupport {
    private final OrderEventStore eventStore;
    private final OrderProjection projection;

    public OrderCommandSupport(OrderEventStore eventStore, OrderProjection projection) {
        this.eventStore = eventStore;
        this.projection = projection;
    }

    public Order loadOwned(UUID orderId, UUID actorId, boolean admin) {
        List<OrderEvent> history = eventStore.load(orderId);
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("Order was not found");
        }
        Order order = Order.rehydrate(history);
        if (!admin && !order.customerId().equals(actorId)) {
            throw new ForbiddenException("You cannot modify this order");
        }
        return order;
    }

    public void persist(Order order, long expectedVersion) {
        List<OrderEvent> events = order.getUncommittedEvents().stream()
                .map(event -> (OrderEvent) event)
                .toList();
        eventStore.append(order.id().value(), expectedVersion, events);
        projection.apply(events);
        order.markEventsAsCommitted();
    }
}

