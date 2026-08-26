package com.johnvo.retailhub.domain.catalog;

import com.johnvo.retailhub.domain.shared.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class Product {
    private final ProductId id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private CategoryId categoryId;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Product(ProductId id, String name, String description, String sku, BigDecimal price,
                    CategoryId categoryId, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);
        changeDetails(name, description, sku, price, categoryId, active, updatedAt);
    }

    public static Product create(ProductId id, String name, String description, String sku,
                                 BigDecimal price, CategoryId categoryId, Instant now) {
        return new Product(id, name, description, sku, price, categoryId, true, now, now);
    }

    public static Product reconstitute(ProductId id, String name, String description, String sku,
                                       BigDecimal price, CategoryId categoryId, boolean active,
                                       Instant createdAt, Instant updatedAt) {
        return new Product(id, name, description, sku, price, categoryId, active, createdAt, updatedAt);
    }

    public void update(String name, String description, String sku, BigDecimal price,
                       CategoryId categoryId, boolean active, Instant now) {
        changeDetails(name, description, sku, price, categoryId, active, now);
    }

    public void deactivate(Instant now) {
        active = false;
        updatedAt = Objects.requireNonNull(now);
    }

    private void changeDetails(String name, String description, String sku, BigDecimal price,
                               CategoryId categoryId, boolean active, Instant updatedAt) {
        if (name == null || name.isBlank() || name.length() > 200) {
            throw new DomainException("Product name must contain between 1 and 200 characters");
        }
        if (sku == null || sku.isBlank() || sku.length() > 80) {
            throw new DomainException("SKU must contain between 1 and 80 characters");
        }
        if (price == null || price.signum() < 0) {
            throw new DomainException("Product price cannot be negative");
        }
        this.name = name.trim();
        this.description = description == null ? "" : description.trim();
        this.sku = sku.trim().toUpperCase(Locale.ROOT);
        this.price = price.setScale(2, java.math.RoundingMode.HALF_UP);
        this.categoryId = Objects.requireNonNull(categoryId, "Category is required");
        this.active = active;
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public ProductId id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public String sku() { return sku; }
    public BigDecimal price() { return price; }
    public CategoryId categoryId() { return categoryId; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}

