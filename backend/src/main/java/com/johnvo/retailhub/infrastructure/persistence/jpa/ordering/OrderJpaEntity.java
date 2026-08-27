package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderItem;
import com.johnvo.retailhub.domain.ordering.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    protected OrderJpaEntity() {
    }

    public OrderJpaEntity(Order order) {
        this.id = order.id().value();
        update(order);
    }

    public void update(Order order) {
        customerId = order.customerId();
        status = order.status();
        createdAt = order.createdAt();
        updatedAt = order.updatedAt();
        confirmedAt = order.confirmedAt();
        cancelledAt = order.cancelledAt();

        Map<UUID, OrderItemJpaEntity> existing = items.stream()
                .collect(Collectors.toMap(OrderItemJpaEntity::getId, item -> item, (left, right) -> left,
                        HashMap::new));
        Set<UUID> retainedIds = order.items().stream().map(OrderItem::id).collect(Collectors.toSet());
        items.removeIf(item -> !retainedIds.contains(item.getId()));
        for (OrderItem item : order.items()) {
            OrderItemJpaEntity entity = existing.get(item.id());
            if (entity == null) {
                items.add(new OrderItemJpaEntity(this, item));
            } else {
                entity.update(item);
            }
        }
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public long getVersion() { return version; }
    public List<OrderItemJpaEntity> getItems() { return items; }
}
