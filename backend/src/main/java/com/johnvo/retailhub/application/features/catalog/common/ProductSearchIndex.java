package com.johnvo.retailhub.application.features.catalog.common;

import com.johnvo.retailhub.application.common.PageResponse;

import java.util.List;
import java.util.UUID;

public interface ProductSearchIndex {
    void index(ProductView product);

    void delete(UUID productId);

    PageResponse<ProductView> search(String query, int page, int size);

    int rebuild(List<ProductView> products);
}

