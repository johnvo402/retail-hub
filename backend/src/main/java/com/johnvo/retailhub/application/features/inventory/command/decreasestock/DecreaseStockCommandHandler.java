package com.johnvo.retailhub.application.features.inventory.command.decreasestock;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementRepository;
import com.johnvo.retailhub.application.features.inventory.common.StockAdjustmentResult;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryMovement;
import com.johnvo.retailhub.domain.inventory.InventoryMovementType;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class DecreaseStockCommandHandler
        implements CommandHandler<DecreaseStockCommand, StockAdjustmentResult> {
    private final InventoryRepository inventory;
    private final InventoryMovementRepository movements;
    private final Clock clock;

    public DecreaseStockCommandHandler(InventoryRepository inventory,
                                       InventoryMovementRepository movements, Clock clock) {
        this.inventory = inventory;
        this.movements = movements;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<StockAdjustmentResult> handle(DecreaseStockCommand command) {
        InventoryItem item = inventory.findByProductId(new ProductId(command.productId())).orElse(null);
        if (item == null) {
            return Result.failure(new ApplicationError(
                    "INVENTORY_NOT_FOUND", "Inventory item was not found", ErrorType.NOT_FOUND));
        }
        try {
            Instant now = clock.instant();
            int quantityBefore = item.quantity();
            item.decrease(command.quantity(), now);
            InventoryMovement movement = InventoryMovement.create(item.productId(),
                    InventoryMovementType.MANUAL_DECREASE, quantityBefore, item.quantity(),
                    command.actorId(), null, command.reason(), now);
            InventoryItem saved = inventory.save(item);
            movements.save(movement);
            return Result.success(new StockAdjustmentResult(
                    saved.productId().value(), saved.quantity(), saved.version()));
        } catch (DomainException exception) {
            return Result.failure(new ApplicationError(
                    "INVENTORY_ADJUSTMENT_INVALID", exception.getMessage(), ErrorType.BUSINESS_RULE));
        }
    }
}
