package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.CategoryView;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.domain.catalog.ProductFilter;
import com.johnvo.retailhub.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaCatalogReadAdapter implements CatalogReadPort {
    private final SpringDataProductRepository products;
    private final SpringDataCategoryRepository categories;
    private final CatalogPersistenceMapper mapper;

    public JpaCatalogReadAdapter(SpringDataProductRepository products,
                                 SpringDataCategoryRepository categories,
                                 CatalogPersistenceMapper mapper) {
        this.products = products;
        this.categories = categories;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProductView> findProduct(UUID id) {
        return products.findDetailedById(id).map(mapper::toView);
    }

    @Override
    public PageResponse<ProductView> findProducts(ProductFilter filter) {
        Page<ProductJpaEntity> page = products.findAll(ProductSpecifications.from(filter),
                PageRequest.of(filter.page(), filter.size(), SortParser.parse(filter.sort(), "createdAt")));
        return new PageResponse<>(page.getContent().stream().map(mapper::toView).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public List<ProductView> findAllActiveProducts() {
        return products.findAllByActiveTrueOrderByCreatedAtDesc().stream().map(mapper::toView).toList();
    }

    @Override
    public Optional<CategoryView> findCategory(UUID id) {
        return categories.findById(id).map(mapper::toView);
    }

    @Override
    public List<CategoryView> findActiveCategories() {
        return categories.findAllByActiveTrueOrderByNameAsc().stream().map(mapper::toView).toList();
    }
}

