package com.johnvo.retailhub.application.features.inventory.command.increasestock;

import com.johnvo.retailhub.application.common.cqrs.Command;
import com.johnvo.retailhub.application.features.inventory.common.StockAdjustmentResult;

import java.util.UUID;

public record IncreaseStockCommand(UUID productId, int quantity, UUID actorId, String reason)
        implements Command<StockAdjustmentResult> {
}
