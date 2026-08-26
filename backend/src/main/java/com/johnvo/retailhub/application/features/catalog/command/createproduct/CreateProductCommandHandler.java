package com.johnvo.retailhub.application.features.catalog.command.createproduct;

import com.johnvo.retailhub.application.common.ConflictException;
import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.ResourceNotFoundException;
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
    public CreatedId handle(CreateProductCommand command) {
        if (products.findBySku(command.sku()).isPresent()) {
            throw new ConflictException("A product with this SKU already exists");
        }
        Category category = categories.findById(new CategoryId(command.categoryId()))
                .filter(Category::active)
                .orElseThrow(() -> new ResourceNotFoundException("Active category was not found"));
        Instant now = clock.instant();
        Product product = Product.create(ProductId.newId(), command.name(), command.description(),
                command.sku(), command.price(), category.id(), now);
        product = products.save(product);
        inventory.save(InventoryItem.create(product.id(), now));
        searchIndex.index(ProductView.from(product, category));
        return new CreatedId(product.id().value());
    }
}

