package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_read_models")
public class OrderReadModelJpaEntity {
    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemReadModelJpaEntity> items = new ArrayList<>();

    protected OrderReadModelJpaEntity() {
    }

    public OrderReadModelJpaEntity(UUID id, UUID customerId, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.DRAFT;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
        this.itemCount = 0;
        this.createdAt = createdAt;
    }

    public void addItem(OrderItemReadModelJpaEntity item) {
        items.add(item);
        recompute();
    }

    public void removeItem(UUID itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
        recompute();
    }

    public void confirm(Instant when) {
        status = OrderStatus.CONFIRMED;
        confirmedAt = when;
    }

    public void cancel(Instant when) {
        status = OrderStatus.CANCELLED;
        cancelledAt = when;
    }

    private void recompute() {
        totalAmount = items.stream().map(OrderItemReadModelJpaEntity::getLineTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        itemCount = items.stream().mapToInt(OrderItemReadModelJpaEntity::getQuantity).sum();
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getItemCount() { return itemCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public List<OrderItemReadModelJpaEntity> getItems() { return items; }
}

