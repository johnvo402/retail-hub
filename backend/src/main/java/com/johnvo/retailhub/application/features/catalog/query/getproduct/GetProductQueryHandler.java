package com.johnvo.retailhub.application.features.catalog.query.getproduct;

import com.johnvo.retailhub.application.common.ResourceNotFoundException;
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
    public ProductView handle(GetProductQuery query) {
        return cache.get(query.id()).orElseGet(() -> {
            ProductView product = catalog.findProduct(query.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Product was not found"));
            cache.put(product);
            return product;
        });
    }
}

