package com.johnvo.retailhub.application.features.catalog.command.updatecategory;

import com.johnvo.retailhub.application.common.ConflictException;
import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class UpdateCategoryCommandHandler implements CommandHandler<UpdateCategoryCommand, Void> {
    private final CategoryRepository categories;
    private final CatalogReadPort catalog;
    private final ProductSearchIndex searchIndex;
    private final Clock clock;

    public UpdateCategoryCommandHandler(CategoryRepository categories, CatalogReadPort catalog,
                                        ProductSearchIndex searchIndex, Clock clock) {
        this.categories = categories;
        this.catalog = catalog;
        this.searchIndex = searchIndex;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Void handle(UpdateCategoryCommand command) {
        Category category = categories.findById(new CategoryId(command.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Category was not found"));
        categories.findByName(command.name())
                .filter(other -> !other.id().equals(category.id()))
                .ifPresent(other -> { throw new ConflictException("A category with this name already exists"); });
        category.update(command.name(), command.description(), command.active(), clock.instant());
        categories.save(category);
        searchIndex.rebuild(catalog.findAllActiveProducts());
        return null;
    }
}

