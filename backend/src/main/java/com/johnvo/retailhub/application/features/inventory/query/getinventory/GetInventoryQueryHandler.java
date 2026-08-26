package com.johnvo.retailhub.application.features.inventory.query.getinventory;

import com.johnvo.retailhub.application.common.ResourceNotFoundException;
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
    public InventoryView handle(GetInventoryQuery query) {
        return inventory.findByProductId(query.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item was not found"));
    }
}

