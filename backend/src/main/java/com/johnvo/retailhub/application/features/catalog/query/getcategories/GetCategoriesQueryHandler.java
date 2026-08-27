package com.johnvo.retailhub.application.features.catalog.query.getcategories;

import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.CategoryView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetCategoriesQueryHandler implements QueryHandler<GetCategoriesQuery, List<CategoryView>> {
    private final CatalogReadPort catalog;

    public GetCategoriesQueryHandler(CatalogReadPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public Result<List<CategoryView>> handle(GetCategoriesQuery query) {
        return Result.success(catalog.findActiveCategories());
    }
}
