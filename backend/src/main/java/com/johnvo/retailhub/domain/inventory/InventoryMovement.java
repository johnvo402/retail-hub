package com.johnvo.retailhub.domain.inventory;

import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.shared.DomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class InventoryMovement {
    public static final int MAX_REASON_LENGTH = 500;

    private final UUID id;
    private final ProductId productId;
    private final InventoryMovementType type;
    private final int quantityDelta;
    private final int quantityBefore;
    private final int quantityAfter;
    private final UUID actorUserId;
    private final UUID referenceId;
    private final String reason;
    private final Instant createdAt;

    private InventoryMovement(UUID id, ProductId productId, InventoryMovementType type,
                              int quantityDelta, int quantityBefore, int quantityAfter,
                              UUID actorUserId, UUID referenceId, String reason, Instant createdAt) {
        if (quantityBefore < 0 || quantityAfter < 0) {
            throw new DomainException("Inventory movement quantities cannot be negative");
        }
        if (quantityDelta == 0) {
            throw new DomainException("Inventory movement quantity delta cannot be zero");
        }
        if ((long) quantityBefore + quantityDelta != quantityAfter) {
            throw new DomainException("Inventory movement quantities do not balance");
        }
        if (type == InventoryMovementType.MANUAL_INCREASE && quantityDelta < 0
                || type != InventoryMovementType.MANUAL_INCREASE && quantityDelta > 0) {
            throw new DomainException("Inventory movement direction does not match its type");
        }
        this.id = Objects.requireNonNull(id);
        this.productId = Objects.requireNonNull(productId);
        this.type = Objects.requireNonNull(type);
        this.quantityDelta = quantityDelta;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.actorUserId = actorUserId;
        this.referenceId = referenceId;
        this.reason = normalizeReason(reason);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static InventoryMovement create(ProductId productId, InventoryMovementType type,
                                           int quantityBefore, int quantityAfter, UUID actorUserId,
                                           UUID referenceId, String reason, Instant createdAt) {
        return new InventoryMovement(UUID.randomUUID(), productId, type,
                Math.subtractExact(quantityAfter, quantityBefore), quantityBefore, quantityAfter,
                actorUserId, referenceId, reason, createdAt);
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.trim();
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new DomainException("Inventory movement reason cannot exceed %d characters"
                    .formatted(MAX_REASON_LENGTH));
        }
        return normalized;
    }

    public UUID id() { return id; }
    public ProductId productId() { return productId; }
    public InventoryMovementType type() { return type; }
    public int quantityDelta() { return quantityDelta; }
    public int quantityBefore() { return quantityBefore; }
    public int quantityAfter() { return quantityAfter; }
    public UUID actorUserId() { return actorUserId; }
    public UUID referenceId() { return referenceId; }
    public String reason() { return reason; }
    public Instant createdAt() { return createdAt; }
}
