package com.johnvo.retailhub.application.features.inventory.common;

import com.johnvo.retailhub.domain.inventory.InventoryMovement;

public interface InventoryMovementRepository {
    void save(InventoryMovement movement);
}
