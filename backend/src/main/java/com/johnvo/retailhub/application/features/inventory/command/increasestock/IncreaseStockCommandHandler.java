package com.johnvo.retailhub.application.features.inventory.command.increasestock;

import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.inventory.common.StockAdjustmentResult;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class IncreaseStockCommandHandler
        implements CommandHandler<IncreaseStockCommand, StockAdjustmentResult> {
    private final InventoryRepository inventory;
    private final Clock clock;

    public IncreaseStockCommandHandler(InventoryRepository inventory, Clock clock) {
        this.inventory = inventory;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StockAdjustmentResult handle(IncreaseStockCommand command) {
        InventoryItem item = inventory.findByProductId(new ProductId(command.productId()))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item was not found"));
        item.increase(command.quantity(), clock.instant());
        InventoryItem saved = inventory.save(item);
        return new StockAdjustmentResult(saved.productId().value(), saved.quantity(), saved.version());
    }
}

