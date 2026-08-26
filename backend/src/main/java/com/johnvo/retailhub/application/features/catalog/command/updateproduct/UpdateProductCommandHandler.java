package com.johnvo.retailhub.application.features.catalog.command.updateproduct;

import com.johnvo.retailhub.application.common.ConflictException;
import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.ProductCache;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
import com.johnvo.retailhub.domain.catalog.Product;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.catalog.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class UpdateProductCommandHandler implements CommandHandler<UpdateProductCommand, Void> {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductCache cache;
    private final ProductSearchIndex searchIndex;
    private final Clock clock;

    public UpdateProductCommandHandler(ProductRepository products, CategoryRepository categories,
                                       ProductCache cache, ProductSearchIndex searchIndex, Clock clock) {
        this.products = products;
        this.categories = categories;
        this.cache = cache;
        this.searchIndex = searchIndex;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Void handle(UpdateProductCommand command) {
        Product product = products.findById(new ProductId(command.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found"));
        products.findBySku(command.sku())
                .filter(other -> !other.id().equals(product.id()))
                .ifPresent(other -> { throw new ConflictException("A product with this SKU already exists"); });
        Category category = categories.findById(new CategoryId(command.categoryId()))
                .filter(Category::active)
                .orElseThrow(() -> new ResourceNotFoundException("Active category was not found"));
        product.update(command.name(), command.description(), command.sku(), command.price(),
                category.id(), command.active(), clock.instant());
        Product saved = products.save(product);
        cache.evict(saved.id().value());
        if (saved.active()) {
            searchIndex.index(ProductView.from(saved, category));
        } else {
            searchIndex.delete(saved.id().value());
        }
        return null;
    }
}

