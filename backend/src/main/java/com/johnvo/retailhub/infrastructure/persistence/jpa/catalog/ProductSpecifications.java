package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import com.johnvo.retailhub.domain.catalog.ProductFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ProductSpecifications {
    private ProductSpecifications() {
    }

    static Specification<ProductJpaEntity> from(ProductFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.minPrice() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }
            if (filter.active() != null) {
                predicates.add(builder.equal(root.get("active"), filter.active()));
            }
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = "%" + filter.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("sku")), pattern),
                        builder.like(builder.lower(root.get("category").get("name")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

