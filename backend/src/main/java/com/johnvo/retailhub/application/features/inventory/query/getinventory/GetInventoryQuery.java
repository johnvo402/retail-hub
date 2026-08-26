package com.johnvo.retailhub.application.features.inventory.query.getinventory;

import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.inventory.common.InventoryView;

import java.util.UUID;

public record GetInventoryQuery(UUID productId) implements Query<InventoryView> {
}

