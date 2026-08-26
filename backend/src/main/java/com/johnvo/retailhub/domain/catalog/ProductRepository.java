package com.johnvo.retailhub.domain.catalog;

import com.johnvo.retailhub.domain.shared.PageResult;

import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(ProductId id);

    Optional<Product> findBySku(String sku);

    PageResult<Product> findAll(ProductFilter filter);

    Product save(Product product);
}
