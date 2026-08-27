package com.johnvo.retailhub.application.features.inventory.query.getinventory;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.inventory.common.InventoryReadPort;
import com.johnvo.retailhub.application.features.inventory.common.InventoryView;
import org.springframework.stereotype.Service;

@Service
public class GetInventoryQueryHandler implements QueryHandler<GetInventoryQuery, InventoryView> {
    private final InventoryReadPort inventory;

    public GetInventoryQueryHandler(InventoryReadPort inventory) {
        this.inventory = inventory;
    }

    @Override
    public Result<InventoryView> handle(GetInventoryQuery query) {
        return inventory.findByProductId(query.productId()).map(Result::success)
                .orElseGet(() -> Result.failure(new ApplicationError(
                        "INVENTORY_NOT_FOUND", "Inventory item was not found", ErrorType.NOT_FOUND)));
    }
}
