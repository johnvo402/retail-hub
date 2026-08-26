package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import com.johnvo.retailhub.infrastructure.persistence.jpa.catalog.ProductJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryJpaEntity implements Persistable<UUID> {
    @Id
    @Column(name = "product_id")
    private UUID productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private ProductJpaEntity product;

    @Column(nullable = false)
    private int quantity;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean newEntity = true;

    protected InventoryJpaEntity() {
    }

    public InventoryJpaEntity(ProductJpaEntity product, int quantity, Instant updatedAt) {
        this.productId = product.getId();
        this.product = product;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    public void update(int quantity, Instant updatedAt) {
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    @Override
    public UUID getId() { return productId; }
    @Override
    public boolean isNew() { return newEntity; }

    @PostLoad
    @PostPersist
    void markPersisted() {
        this.newEntity = false;
    }

    public UUID getProductId() { return productId; }
    public ProductJpaEntity getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}
