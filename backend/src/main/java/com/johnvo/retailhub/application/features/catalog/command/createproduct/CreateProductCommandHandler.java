package com.johnvo.retailhub.application.features.catalog.command.createproduct;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
import com.johnvo.retailhub.domain.catalog.Product;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.catalog.ProductRepository;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, CreatedId> {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryRepository inventory;
    private final ProductSearchIndex searchIndex;
    private final Clock clock;

    public CreateProductCommandHandler(ProductRepository products, CategoryRepository categories,
                                       InventoryRepository inventory, ProductSearchIndex searchIndex,
                                       Clock clock) {
        this.products = products;
        this.categories = categories;
        this.inventory = inventory;
        this.searchIndex = searchIndex;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<CreatedId> handle(CreateProductCommand command) {
        if (products.findBySku(command.sku()).isPresent()) {
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
            Instant now = clock.instant();
            Product product = Product.create(ProductId.newId(), command.name(), command.description(),
                    command.sku(), command.price(), category.id(), now);
            product = products.save(product);
            inventory.save(InventoryItem.create(product.id(), now));
            searchIndex.index(ProductView.from(product, category));
            return Result.success(new CreatedId(product.id().value()));
        } catch (DomainException exception) {
            return Result.failure(new ApplicationError(
                    "PRODUCT_INVALID", exception.getMessage(), ErrorType.VALIDATION));
        }
    }
}
