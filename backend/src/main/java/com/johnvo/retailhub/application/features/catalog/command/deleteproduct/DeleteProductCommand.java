package com.johnvo.retailhub.application.features.catalog.command.deleteproduct;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record DeleteProductCommand(UUID id) implements Command<Void> {
}

