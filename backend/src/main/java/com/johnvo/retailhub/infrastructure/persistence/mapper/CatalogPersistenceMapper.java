package com.johnvo.retailhub.infrastructure.persistence.mapper;

import com.johnvo.retailhub.application.features.catalog.common.CategoryView;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.infrastructure.persistence.jpa.catalog.CategoryJpaEntity;
import com.johnvo.retailhub.infrastructure.persistence.jpa.catalog.ProductJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogPersistenceMapper {
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    ProductView toView(ProductJpaEntity entity);

    CategoryView toView(CategoryJpaEntity entity);
}

