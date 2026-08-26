package com.johnvo.retailhub.application.features.ordering.command.createorder;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record CreateOrderCommand(UUID customerId) implements Command<CreatedId> {
}

