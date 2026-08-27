package com.johnvo.retailhub.application.features.catalog.query.getproduct;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.ProductCache;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import org.springframework.stereotype.Service;

@Service
public class GetProductQueryHandler implements QueryHandler<GetProductQuery, ProductView> {
    private final CatalogReadPort catalog;
    private final ProductCache cache;

    public GetProductQueryHandler(CatalogReadPort catalog, ProductCache cache) {
        this.catalog = catalog;
        this.cache = cache;
    }

    @Override
    public Result<ProductView> handle(GetProductQuery query) {
        ProductView cached = cache.get(query.id()).orElse(null);
        if (cached != null) {
            return Result.success(cached);
        }
        ProductView product = catalog.findProduct(query.id()).orElse(null);
        if (product == null) {
            return Result.failure(new ApplicationError(
                    "PRODUCT_NOT_FOUND", "Product was not found", ErrorType.NOT_FOUND));
        }
        cache.put(product);
        return Result.success(product);
    }
}
