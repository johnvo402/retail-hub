package com.johnvo.retailhub.application.features.catalog.command.updateproduct;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
        UUID id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        UUID categoryId,
        boolean active
) implements Command<Void> {
}

