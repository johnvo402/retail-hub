package com.johnvo.retailhub.application.features.ordering.command.cancelorder;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId, UUID actorId, boolean admin) implements Command<Void> {
}

