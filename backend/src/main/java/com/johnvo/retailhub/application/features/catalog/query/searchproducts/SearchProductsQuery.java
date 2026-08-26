package com.johnvo.retailhub.application.features.catalog.query.searchproducts;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;

public record SearchProductsQuery(String query, int page, int size)
        implements Query<PageResponse<ProductView>> {
}

