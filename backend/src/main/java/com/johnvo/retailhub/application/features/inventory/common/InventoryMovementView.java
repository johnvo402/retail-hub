package com.johnvo.retailhub.application.features.inventory.common;

import com.johnvo.retailhub.domain.inventory.InventoryMovementType;

import java.time.Instant;
import java.util.UUID;

public record InventoryMovementView(
        UUID id,
        UUID productId,
        InventoryMovementType type,
        int quantityDelta,
        int quantityBefore,
        int quantityAfter,
        UUID actorUserId,
        UUID referenceId,
        String reason,
        Instant createdAt
) {
}
