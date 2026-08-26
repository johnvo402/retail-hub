package com.johnvo.retailhub.application.features.catalog.command.updatecategory;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record UpdateCategoryCommand(UUID id, String name, String description, boolean active)
        implements Command<Void> {
}

