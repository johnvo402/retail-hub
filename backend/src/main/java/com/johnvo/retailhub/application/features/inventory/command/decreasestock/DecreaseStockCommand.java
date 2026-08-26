package com.johnvo.retailhub.application.features.inventory.command.decreasestock;

import com.johnvo.retailhub.application.common.cqrs.Command;
import com.johnvo.retailhub.application.features.inventory.common.StockAdjustmentResult;

import java.util.UUID;

public record DecreaseStockCommand(UUID productId, int quantity)
        implements Command<StockAdjustmentResult> {
}

