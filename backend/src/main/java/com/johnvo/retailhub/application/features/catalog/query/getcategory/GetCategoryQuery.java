package com.johnvo.retailhub.application.features.catalog.query.getcategory;

import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.catalog.common.CategoryView;

import java.util.UUID;

public record GetCategoryQuery(UUID id) implements Query<CategoryView> {
}

