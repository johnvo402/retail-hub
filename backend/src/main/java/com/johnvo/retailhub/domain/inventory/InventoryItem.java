package com.johnvo.retailhub.domain.inventory;

import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.shared.DomainException;

import java.time.Instant;
import java.util.Objects;

public final class InventoryItem {
    private final ProductId productId;
    private int quantity;
    private long version;
    private Instant updatedAt;

    private InventoryItem(ProductId productId, int quantity, long version, Instant updatedAt) {
        if (quantity < 0) {
            throw new DomainException("Inventory quantity cannot be negative");
        }
        this.productId = Objects.requireNonNull(productId);
        this.quantity = quantity;
        this.version = version;
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static InventoryItem create(ProductId productId, Instant now) {
        return new InventoryItem(productId, 0, 0, now);
    }

    public static InventoryItem reconstitute(ProductId productId, int quantity, long version, Instant updatedAt) {
        return new InventoryItem(productId, quantity, version, updatedAt);
    }

    public void increase(int amount, Instant now) {
        requirePositive(amount);
        quantity = Math.addExact(quantity, amount);
        updatedAt = Objects.requireNonNull(now);
    }

    public void decrease(int amount, Instant now) {
        requirePositive(amount);
        if (quantity < amount) {
            throw new DomainException("Insufficient stock: requested %d, available %d".formatted(amount, quantity));
        }
        quantity -= amount;
        updatedAt = Objects.requireNonNull(now);
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new DomainException("Stock adjustment quantity must be greater than zero");
        }
    }

    public ProductId productId() { return productId; }
    public int quantity() { return quantity; }
    public long version() { return version; }
    public Instant updatedAt() { return updatedAt; }
}

