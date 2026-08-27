package com.johnvo.retailhub.application.features.catalog.command.updatecategory;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
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
    public Result<Void> handle(UpdateCategoryCommand command) {
        Category category = categories.findById(new CategoryId(command.id())).orElse(null);
        if (category == null) {
            return Result.failure(new ApplicationError(
                    "CATEGORY_NOT_FOUND", "Category was not found", ErrorType.NOT_FOUND));
        }
        if (categories.findByName(command.name()).filter(other -> !other.id().equals(category.id())).isPresent()) {
            return Result.failure(new ApplicationError("CATEGORY_NAME_EXISTS",
                    "A category with this name already exists", ErrorType.CONFLICT));
        }
        try {
            category.update(command.name(), command.description(), command.active(), clock.instant());
            categories.save(category);
            searchIndex.rebuild(catalog.findAllActiveProducts());
            return Result.success();
        } catch (DomainException exception) {
            return Result.failure(new ApplicationError(
                    "CATEGORY_INVALID", exception.getMessage(), ErrorType.VALIDATION));
        }
    }
}
