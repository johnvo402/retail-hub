package com.johnvo.retailhub.application.features.catalog.command.reindexproducts;

import com.johnvo.retailhub.application.common.cqrs.Command;

public record ReindexProductsCommand() implements Command<Integer> {
}

