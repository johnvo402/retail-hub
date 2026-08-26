package com.johnvo.retailhub.application.features.inventory.query.getinventoryitems;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.inventory.common.InventoryView;

public record GetInventoryItemsQuery(int page, int size, String sort)
        implements Query<PageResponse<InventoryView>> {
}

