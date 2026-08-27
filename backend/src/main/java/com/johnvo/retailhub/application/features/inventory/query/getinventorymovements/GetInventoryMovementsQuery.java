package com.johnvo.retailhub.application.features.inventory.query.getinventorymovements;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementView;

import java.util.UUID;

public record GetInventoryMovementsQuery(UUID productId, int page, int size)
        implements Query<PageResponse<InventoryMovementView>> {
}
