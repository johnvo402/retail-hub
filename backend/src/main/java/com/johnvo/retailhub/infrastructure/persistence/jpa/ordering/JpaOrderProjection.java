package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import com.johnvo.retailhub.application.features.ordering.common.OrderProjection;
import com.johnvo.retailhub.domain.ordering.events.OrderCancelled;
import com.johnvo.retailhub.domain.ordering.events.OrderConfirmed;
import com.johnvo.retailhub.domain.ordering.events.OrderCreated;
import com.johnvo.retailhub.domain.ordering.events.OrderEvent;
import com.johnvo.retailhub.domain.ordering.events.OrderItemAdded;
import com.johnvo.retailhub.domain.ordering.events.OrderItemRemoved;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaOrderProjection implements OrderProjection {
    private final SpringDataOrderReadRepository repository;

    public JpaOrderProjection(SpringDataOrderReadRepository repository) {
        this.repository = repository;
    }

    @Override
    public void apply(List<OrderEvent> events) {
        for (OrderEvent event : events) {
            if (event instanceof OrderCreated created) {
                repository.save(new OrderReadModelJpaEntity(created.aggregateId(), created.customerId(),
                        created.occurredAt()));
                continue;
            }
            OrderReadModelJpaEntity order = repository.findDetailedById(event.aggregateId())
                    .orElseThrow(() -> new IllegalStateException("Order projection does not exist"));
            if (event instanceof OrderItemAdded added) {
                order.addItem(new OrderItemReadModelJpaEntity(added.itemId(), order, added.productId(),
                        added.productName(), added.sku(), added.unitPrice(), added.quantity()));
            } else if (event instanceof OrderItemRemoved removed) {
                order.removeItem(removed.itemId());
            } else if (event instanceof OrderConfirmed confirmed) {
                order.confirm(confirmed.occurredAt());
            } else if (event instanceof OrderCancelled cancelled) {
                order.cancel(cancelled.occurredAt());
            }
            repository.save(order);
        }
    }
}

