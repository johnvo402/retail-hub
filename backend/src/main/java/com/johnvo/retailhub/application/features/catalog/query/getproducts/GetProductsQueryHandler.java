package com.johnvo.retailhub.application.features.catalog.query.getproducts;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import org.springframework.stereotype.Service;

@Service
public class GetProductsQueryHandler implements QueryHandler<GetProductsQuery, PageResponse<ProductView>> {
    private final CatalogReadPort catalog;

    public GetProductsQueryHandler(CatalogReadPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public Result<PageResponse<ProductView>> handle(GetProductsQuery query) {
        return Result.success(catalog.findProducts(query.filter()));
    }
}
