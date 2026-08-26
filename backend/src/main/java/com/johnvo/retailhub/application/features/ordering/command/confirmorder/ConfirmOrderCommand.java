package com.johnvo.retailhub.application.features.ordering.command.confirmorder;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record ConfirmOrderCommand(UUID orderId, UUID actorId, boolean admin) implements Command<Void> {
}

