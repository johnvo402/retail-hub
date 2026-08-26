package com.johnvo.retailhub.application.features.catalog.query.getcategory;

import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.CategoryView;
import org.springframework.stereotype.Service;

@Service
public class GetCategoryQueryHandler implements QueryHandler<GetCategoryQuery, CategoryView> {
    private final CatalogReadPort catalog;

    public GetCategoryQueryHandler(CatalogReadPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public CategoryView handle(GetCategoryQuery query) {
        return catalog.findCategory(query.id())
                .orElseThrow(() -> new ResourceNotFoundException("Category was not found"));
    }
}

