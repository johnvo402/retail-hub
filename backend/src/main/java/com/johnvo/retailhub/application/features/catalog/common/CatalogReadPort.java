package com.johnvo.retailhub.application.features.catalog.common;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.domain.catalog.ProductFilter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogReadPort {
    Optional<ProductView> findProduct(UUID id);

    PageResponse<ProductView> findProducts(ProductFilter filter);

    List<ProductView> findAllActiveProducts();

    Optional<CategoryView> findCategory(UUID id);

    List<CategoryView> findActiveCategories();
}

