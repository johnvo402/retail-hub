package com.johnvo.retailhub.domain.ordering;

import com.johnvo.retailhub.domain.ordering.events.OrderCancelled;
import com.johnvo.retailhub.domain.ordering.events.OrderConfirmed;
import com.johnvo.retailhub.domain.ordering.events.OrderCreated;
import com.johnvo.retailhub.domain.ordering.events.OrderEvent;
import com.johnvo.retailhub.domain.ordering.events.OrderItemAdded;
import com.johnvo.retailhub.domain.ordering.events.OrderItemRemoved;
import com.johnvo.retailhub.domain.shared.AggregateRoot;
import com.johnvo.retailhub.domain.shared.DomainEvent;
import com.johnvo.retailhub.domain.shared.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Order extends AggregateRoot {
    private OrderId id;
    private UUID customerId;
    private OrderStatus status;
    private final Map<UUID, OrderItem> items = new LinkedHashMap<>();

    private Order() {
    }

    public static Order create(OrderId id, UUID customerId, Instant now) {
        Order order = new Order();
        order.raise(new OrderCreated(id.value(), customerId, now));
        return order;
    }

    public static Order rehydrate(List<? extends OrderEvent> history) {
        if (history == null || history.isEmpty()) {
            throw new DomainException("Cannot rehydrate an order without events");
        }
        Order order = new Order();
        history.forEach(order::replay);
        order.markEventsAsCommitted();
        return order;
    }

    public UUID addItem(UUID productId, String productName, String sku,
                        BigDecimal unitPrice, int quantity, Instant now) {
        ensureDraft();
        UUID itemId = UUID.randomUUID();
        raise(new OrderItemAdded(id.value(), itemId, productId, productName, sku,
                unitPrice, quantity, now));
        return itemId;
    }

    public void removeItem(UUID itemId, Instant now) {
        ensureDraft();
        if (!items.containsKey(itemId)) {
            throw new DomainException("Order item does not exist");
        }
        raise(new OrderItemRemoved(id.value(), itemId, now));
    }

    public void confirm(Instant now) {
        ensureDraft();
        if (items.isEmpty()) {
            throw new DomainException("Cannot confirm an order without items");
        }
        raise(new OrderConfirmed(id.value(), now));
    }

    public void cancel(Instant now) {
        ensureDraft();
        raise(new OrderCancelled(id.value(), now));
    }

    private void ensureDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new DomainException("Only a draft order can be modified");
        }
    }

    @Override
    protected void apply(DomainEvent event) {
        if (event instanceof OrderCreated created) {
            id = new OrderId(created.aggregateId());
            customerId = created.customerId();
            status = OrderStatus.DRAFT;
        } else if (event instanceof OrderItemAdded added) {
            items.put(added.itemId(), new OrderItem(added.itemId(), added.productId(),
                    added.productName(), added.sku(), added.unitPrice(), added.quantity()));
        } else if (event instanceof OrderItemRemoved removed) {
            items.remove(removed.itemId());
        } else if (event instanceof OrderConfirmed) {
            status = OrderStatus.CONFIRMED;
        } else if (event instanceof OrderCancelled) {
            status = OrderStatus.CANCELLED;
        } else {
            throw new DomainException("Unsupported order event: " + event.getClass().getName());
        }
    }

    public OrderId id() { return id; }
    public UUID customerId() { return customerId; }
    public OrderStatus status() { return status; }
    public List<OrderItem> items() { return List.copyOf(items.values()); }
    public BigDecimal totalAmount() {
        return items.values().stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

