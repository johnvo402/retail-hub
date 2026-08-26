package com.johnvo.retailhub.application.features.catalog.query.getcategories;

import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.catalog.common.CategoryView;

import java.util.List;

public record GetCategoriesQuery() implements Query<List<CategoryView>> {
}

