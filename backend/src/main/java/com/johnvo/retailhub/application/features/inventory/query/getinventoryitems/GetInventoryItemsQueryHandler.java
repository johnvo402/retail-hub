package com.johnvo.retailhub.application.features.inventory.query.getinventoryitems;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.inventory.common.InventoryReadPort;
import com.johnvo.retailhub.application.features.inventory.common.InventoryView;
import org.springframework.stereotype.Service;

@Service
public class GetInventoryItemsQueryHandler
        implements QueryHandler<GetInventoryItemsQuery, PageResponse<InventoryView>> {
    private final InventoryReadPort inventory;

    public GetInventoryItemsQueryHandler(InventoryReadPort inventory) {
        this.inventory = inventory;
    }

    @Override
    public Result<PageResponse<InventoryView>> handle(GetInventoryItemsQuery query) {
        return Result.success(inventory.findAll(Math.max(query.page(), 0),
                Math.min(Math.max(query.size(), 1), 100), query.sort()));
    }
}
