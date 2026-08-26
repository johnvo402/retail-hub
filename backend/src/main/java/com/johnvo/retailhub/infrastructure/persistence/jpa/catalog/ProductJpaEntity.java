package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryJpaEntity category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(UUID id) {
        this.id = id;
    }

    public void update(CategoryJpaEntity category, String name, String description, String sku,
                       java.math.BigDecimal price, boolean active, Instant createdAt, Instant updatedAt) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.price = price;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public CategoryJpaEntity getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSku() { return sku; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

