package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.Product;
import com.johnvo.retailhub.domain.catalog.ProductFilter;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.catalog.ProductRepository;
import com.johnvo.retailhub.domain.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaProductRepositoryAdapter implements ProductRepository {
    private final SpringDataProductRepository products;
    private final SpringDataCategoryRepository categories;

    public JpaProductRepositoryAdapter(SpringDataProductRepository products,
                                       SpringDataCategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return products.findDetailedById(id.value()).map(JpaProductRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return products.findBySkuIgnoreCase(sku).map(JpaProductRepositoryAdapter::toDomain);
    }

    @Override
    public PageResult<Product> findAll(ProductFilter filter) {
        Page<ProductJpaEntity> page = products.findAll(ProductSpecifications.from(filter),
                PageRequest.of(filter.page(), filter.size(), SortParser.parse(filter.sort(), "createdAt")));
        return new PageResult<>(page.getContent().stream().map(JpaProductRepositoryAdapter::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = products.findById(product.id().value())
                .orElseGet(() -> new ProductJpaEntity(product.id().value()));
        CategoryJpaEntity category = categories.getReferenceById(product.categoryId().value());
        entity.update(category, product.name(), product.description(), product.sku(), product.price(),
                product.active(), product.createdAt(), product.updatedAt());
        return toDomain(products.save(entity));
    }

    static Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(new ProductId(entity.getId()), entity.getName(), entity.getDescription(),
                entity.getSku(), entity.getPrice(), new CategoryId(entity.getCategory().getId()),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

