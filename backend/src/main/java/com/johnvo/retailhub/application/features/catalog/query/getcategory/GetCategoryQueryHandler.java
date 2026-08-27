package com.johnvo.retailhub.application.features.catalog.query.getcategory;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
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
    public Result<CategoryView> handle(GetCategoryQuery query) {
        return catalog.findCategory(query.id()).map(Result::success)
                .orElseGet(() -> Result.failure(new ApplicationError(
                        "CATEGORY_NOT_FOUND", "Category was not found", ErrorType.NOT_FOUND)));
    }
}
