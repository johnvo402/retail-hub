package com.johnvo.retailhub.application.features.catalog.query.getproduct;

import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;

import java.util.UUID;

public record GetProductQuery(UUID id) implements Query<ProductView> {
}

