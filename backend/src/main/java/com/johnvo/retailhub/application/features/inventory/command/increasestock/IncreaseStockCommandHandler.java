package com.johnvo.retailhub.application.features.inventory.command.increasestock;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.inventory.common.StockAdjustmentResult;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
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
    public Result<StockAdjustmentResult> handle(IncreaseStockCommand command) {
        InventoryItem item = inventory.findByProductId(new ProductId(command.productId())).orElse(null);
        if (item == null) {
            return Result.failure(new ApplicationError(
                    "INVENTORY_NOT_FOUND", "Inventory item was not found", ErrorType.NOT_FOUND));
        }
        try {
            item.increase(command.quantity(), clock.instant());
            InventoryItem saved = inventory.save(item);
            return Result.success(new StockAdjustmentResult(
                    saved.productId().value(), saved.quantity(), saved.version()));
        } catch (DomainException exception) {
            return Result.failure(new ApplicationError(
                    "INVENTORY_ADJUSTMENT_INVALID", exception.getMessage(), ErrorType.BUSINESS_RULE));
        }
    }
}
