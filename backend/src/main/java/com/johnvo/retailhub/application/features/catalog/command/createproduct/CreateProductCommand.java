package com.johnvo.retailhub.application.features.catalog.command.createproduct;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.cqrs.Command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
        String name,
        String description,
        String sku,
        BigDecimal price,
        UUID categoryId
) implements Command<CreatedId> {
}

