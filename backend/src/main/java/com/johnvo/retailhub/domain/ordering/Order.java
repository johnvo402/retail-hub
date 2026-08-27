package com.johnvo.retailhub.domain.ordering;

import com.johnvo.retailhub.domain.shared.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Order {
    private OrderId id;
    private UUID customerId;
    private OrderStatus status;
    private final Map<UUID, OrderItem> items = new LinkedHashMap<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;
    private Instant cancelledAt;

    private Order() {
    }

    public static Order create(OrderId id, UUID customerId, Instant now) {
        Order order = new Order();
        order.id = Objects.requireNonNull(id, "Order id is required");
        order.customerId = Objects.requireNonNull(customerId, "Customer id is required");
        order.status = OrderStatus.DRAFT;
        order.createdAt = Objects.requireNonNull(now, "Creation time is required");
        order.updatedAt = now;
        return order;
    }

    public static Order reconstitute(OrderId id, UUID customerId, OrderStatus status,
                                     Collection<OrderItem> items, Instant createdAt, Instant updatedAt,
                                     Instant confirmedAt, Instant cancelledAt) {
        Order order = new Order();
        order.id = Objects.requireNonNull(id, "Order id is required");
        order.customerId = Objects.requireNonNull(customerId, "Customer id is required");
        order.status = Objects.requireNonNull(status, "Order status is required");
        Objects.requireNonNull(items, "Order items are required")
                .forEach(item -> order.items.put(item.id(), item));
        order.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        order.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
        order.confirmedAt = confirmedAt;
        order.cancelledAt = cancelledAt;
        return order;
    }

    public UUID addItem(UUID productId, String productName, String sku,
                        BigDecimal unitPrice, int quantity, Instant now) {
        ensureDraft();
        UUID itemId = UUID.randomUUID();
        OrderItem item = new OrderItem(itemId, productId, productName, sku, unitPrice, quantity);
        items.put(itemId, item);
        updatedAt = Objects.requireNonNull(now, "Update time is required");
        return itemId;
    }

    public void removeItem(UUID itemId, Instant now) {
        ensureDraft();
        if (!items.containsKey(itemId)) {
            throw new DomainException("Order item does not exist");
        }
        items.remove(itemId);
        updatedAt = Objects.requireNonNull(now, "Update time is required");
    }

    public void confirm(Instant now) {
        ensureDraft();
        if (items.isEmpty()) {
            throw new DomainException("Cannot confirm an order without items");
        }
        confirmedAt = Objects.requireNonNull(now, "Confirmation time is required");
        updatedAt = now;
        status = OrderStatus.CONFIRMED;
    }

    public void cancel(Instant now) {
        ensureDraft();
        cancelledAt = Objects.requireNonNull(now, "Cancellation time is required");
        updatedAt = now;
        status = OrderStatus.CANCELLED;
    }

    private void ensureDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new DomainException("Only a draft order can be modified");
        }
    }

    public OrderId id() { return id; }
    public UUID customerId() { return customerId; }
    public OrderStatus status() { return status; }
    public List<OrderItem> items() { return List.copyOf(items.values()); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant confirmedAt() { return confirmedAt; }
    public Instant cancelledAt() { return cancelledAt; }
    public BigDecimal totalAmount() {
        return items.values().stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
