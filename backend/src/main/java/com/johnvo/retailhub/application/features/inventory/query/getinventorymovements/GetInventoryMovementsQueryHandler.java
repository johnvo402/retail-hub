package com.johnvo.retailhub.application.features.inventory.query.getinventorymovements;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementReadPort;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementView;
import com.johnvo.retailhub.application.features.inventory.common.InventoryReadPort;
import org.springframework.stereotype.Service;

@Service
public class GetInventoryMovementsQueryHandler
        implements QueryHandler<GetInventoryMovementsQuery, PageResponse<InventoryMovementView>> {
    private final InventoryReadPort inventory;
    private final InventoryMovementReadPort movements;

    public GetInventoryMovementsQueryHandler(InventoryReadPort inventory,
                                             InventoryMovementReadPort movements) {
        this.inventory = inventory;
        this.movements = movements;
    }

    @Override
    public Result<PageResponse<InventoryMovementView>> handle(GetInventoryMovementsQuery query) {
        if (inventory.findByProductId(query.productId()).isEmpty()) {
            return Result.failure(new ApplicationError(
                    "INVENTORY_NOT_FOUND", "Inventory item was not found", ErrorType.NOT_FOUND));
        }
        return Result.success(movements.findByProductId(query.productId(), Math.max(query.page(), 0),
                Math.min(Math.max(query.size(), 1), 100)));
    }
}
