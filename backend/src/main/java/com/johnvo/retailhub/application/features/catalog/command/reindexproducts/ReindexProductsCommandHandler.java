package com.johnvo.retailhub.application.features.catalog.command.reindexproducts;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import org.springframework.stereotype.Service;

@Service
public class ReindexProductsCommandHandler implements CommandHandler<ReindexProductsCommand, Integer> {
    private final CatalogReadPort catalog;
    private final ProductSearchIndex searchIndex;

    public ReindexProductsCommandHandler(CatalogReadPort catalog, ProductSearchIndex searchIndex) {
        this.catalog = catalog;
        this.searchIndex = searchIndex;
    }

    @Override
    public Result<Integer> handle(ReindexProductsCommand command) {
        return Result.success(searchIndex.rebuild(catalog.findAllActiveProducts()));
    }
}
