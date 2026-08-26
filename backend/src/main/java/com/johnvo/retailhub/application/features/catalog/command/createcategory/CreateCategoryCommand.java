package com.johnvo.retailhub.application.features.catalog.command.createcategory;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.cqrs.Command;

public record CreateCategoryCommand(String name, String description) implements Command<CreatedId> {
}

