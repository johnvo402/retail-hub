package com.johnvo.retailhub.application.features.ordering.command.removeorderitem;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record RemoveOrderItemCommand(UUID orderId, UUID itemId, UUID actorId, boolean admin)
        implements Command<Void> {
}

