package com.johnvo.retailhub.application.features.catalog.query.getproducts;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.domain.catalog.ProductFilter;

public record GetProductsQuery(
        ProductFilter filter,
        boolean includeInactive
) implements Query<PageResponse<ProductView>> {
}
