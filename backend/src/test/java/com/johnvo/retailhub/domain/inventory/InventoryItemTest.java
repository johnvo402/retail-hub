package com.johnvo.retailhub.domain.inventory;

import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {
    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

    @Test
    void inventoryCannotGoNegative() {
        InventoryItem item = InventoryItem.create(ProductId.newId(), NOW);
        item.increase(5, NOW);

        assertThatThrownBy(() -> item.decrease(6, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Insufficient stock");
        assertThat(item.quantity()).isEqualTo(5);
    }

    @Test
    void adjustmentMustBePositive() {
        InventoryItem item = InventoryItem.create(ProductId.newId(), NOW);

        assertThatThrownBy(() -> item.increase(0, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("greater than zero");
    }
}

