package com.johnvo.retailhub.domain.inventory;

import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryMovementTest {
    private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");

    @Test
    void blankReasonIsNormalizedToNull() {
        InventoryMovement movement = InventoryMovement.create(ProductId.newId(),
                InventoryMovementType.MANUAL_INCREASE, 10, 15, null, null, "   ", NOW);

        assertThat(movement.reason()).isNull();
    }

    @Test
    void movementDirectionMustMatchType() {
        assertThatThrownBy(() -> InventoryMovement.create(ProductId.newId(),
                InventoryMovementType.MANUAL_DECREASE, 10, 15, null, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("direction");
    }
}
