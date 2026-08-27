package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import com.johnvo.retailhub.domain.inventory.InventoryMovement;
import com.johnvo.retailhub.domain.inventory.InventoryMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovementJpaEntity {
    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private InventoryMovementType type;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "quantity_before", nullable = false)
    private int quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(length = InventoryMovement.MAX_REASON_LENGTH)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryMovementJpaEntity() {
    }

    public InventoryMovementJpaEntity(InventoryMovement movement) {
        this.id = movement.id();
        this.productId = movement.productId().value();
        this.type = movement.type();
        this.quantityDelta = movement.quantityDelta();
        this.quantityBefore = movement.quantityBefore();
        this.quantityAfter = movement.quantityAfter();
        this.actorUserId = movement.actorUserId();
        this.referenceId = movement.referenceId();
        this.reason = movement.reason();
        this.createdAt = movement.createdAt();
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public InventoryMovementType getType() { return type; }
    public int getQuantityDelta() { return quantityDelta; }
    public int getQuantityBefore() { return quantityBefore; }
    public int getQuantityAfter() { return quantityAfter; }
    public UUID getActorUserId() { return actorUserId; }
    public UUID getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
