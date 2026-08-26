package com.johnvo.retailhub.application.features.ordering.command.addorderitem;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record AddOrderItemCommand(
        UUID orderId,
        UUID productId,
        int quantity,
        UUID actorId,
        boolean admin
) implements Command<CreatedId> {
}

