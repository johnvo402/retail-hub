package com.johnvo.retailhub.application.features.inventory.common;

import com.johnvo.retailhub.application.common.PageResponse;

import java.util.UUID;

public interface InventoryMovementReadPort {
    PageResponse<InventoryMovementView> findByProductId(UUID productId, int page, int size);
}
