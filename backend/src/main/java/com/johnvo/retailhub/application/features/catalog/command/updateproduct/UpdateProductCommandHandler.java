package com.johnvo.retailhub.application.features.catalog.command.updateproduct;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
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
import com.johnvo.retailhub.domain.shared.DomainException;
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
    public Result<Void> handle(UpdateProductCommand command) {
        Product product = products.findById(new ProductId(command.id())).orElse(null);
        if (product == null) {
            return Result.failure(new ApplicationError(
                    "PRODUCT_NOT_FOUND", "Product was not found", ErrorType.NOT_FOUND));
        }
        if (products.findBySku(command.sku()).filter(other -> !other.id().equals(product.id())).isPresent()) {
            return Result.failure(new ApplicationError("PRODUCT_SKU_EXISTS",
                    "A product with this SKU already exists", ErrorType.CONFLICT));
        }
        Category category = categories.findById(new CategoryId(command.categoryId()))
                .filter(Category::active).orElse(null);
        if (category == null) {
            return Result.failure(new ApplicationError(
                    "CATEGORY_NOT_FOUND", "Active category was not found", ErrorType.NOT_FOUND));
        }
        try {
            product.update(command.name(), command.description(), command.sku(), command.price(),
                    category.id(), command.active(), clock.instant());
            Product saved = products.save(product);
            cache.evict(saved.id().value());
            if (saved.active()) {
                searchIndex.index(ProductView.from(saved, category));
            } else {
                searchIndex.delete(saved.id().value());
            }
            return Result.success();
        } catch (DomainException exception) {
            return Result.failure(new ApplicationError(
                    "PRODUCT_INVALID", exception.getMessage(), ErrorType.VALIDATION));
        }
    }
}
