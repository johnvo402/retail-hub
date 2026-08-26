package com.johnvo.retailhub.infrastructure.elasticsearch;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ElasticsearchProductSearchAdapter implements ProductSearchIndex {
    private final SpringDataProductSearchRepository repository;

    public ElasticsearchProductSearchAdapter(SpringDataProductSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public void index(ProductView product) {
        repository.save(toDocument(product));
    }

    @Override
    public void delete(UUID productId) {
        repository.deleteById(productId);
    }

    @Override
    public PageResponse<ProductView> search(String query, int page, int size) {
        Page<ProductSearchDocument> result = query == null || query.isBlank()
                ? repository.findAllByActiveTrue(PageRequest.of(page, size))
                : repository.search(query.trim(), PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(ElasticsearchProductSearchAdapter::toView).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public int rebuild(List<ProductView> products) {
        repository.deleteAll();
        repository.saveAll(products.stream().map(ElasticsearchProductSearchAdapter::toDocument).toList());
        return products.size();
    }

    private static ProductSearchDocument toDocument(ProductView product) {
        return new ProductSearchDocument(product.id(), product.name(), product.description(), product.sku(),
                product.price(), product.categoryId(), product.categoryName(), product.active(),
                product.createdAt(), product.updatedAt());
    }

    private static ProductView toView(ProductSearchDocument document) {
        return new ProductView(document.getId(), document.getName(), document.getDescription(),
                document.getSku(), document.getPrice(), document.getCategoryId(), document.getCategoryName(),
                document.isActive(), document.getCreatedAt(), document.getUpdatedAt());
    }
}

