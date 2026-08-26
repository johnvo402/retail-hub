package com.johnvo.retailhub.application.features.inventory.common;

import com.johnvo.retailhub.application.common.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface InventoryReadPort {
    Optional<InventoryView> findByProductId(UUID productId);

    PageResponse<InventoryView> findAll(int page, int size, String sort);
}

