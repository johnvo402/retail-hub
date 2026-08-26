package com.johnvo.retailhub.application.features.catalog.command.deletecategory;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record DeleteCategoryCommand(UUID id) implements Command<Void> {
}

