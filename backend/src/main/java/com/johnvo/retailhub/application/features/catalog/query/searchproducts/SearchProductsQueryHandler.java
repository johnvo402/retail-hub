package com.johnvo.retailhub.application.features.catalog.query.searchproducts;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import org.springframework.stereotype.Service;

@Service
public class SearchProductsQueryHandler implements QueryHandler<SearchProductsQuery, PageResponse<ProductView>> {
    private final ProductSearchIndex searchIndex;

    public SearchProductsQueryHandler(ProductSearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    @Override
    public Result<PageResponse<ProductView>> handle(SearchProductsQuery query) {
        return Result.success(searchIndex.search(query.query(), Math.max(query.page(), 0),
                Math.min(Math.max(query.size(), 1), 100)));
    }
}
