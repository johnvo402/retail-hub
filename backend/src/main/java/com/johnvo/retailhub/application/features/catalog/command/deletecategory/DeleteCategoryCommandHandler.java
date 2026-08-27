package com.johnvo.retailhub.application.features.catalog.command.deletecategory;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
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
public class DeleteCategoryCommandHandler implements CommandHandler<DeleteCategoryCommand, Void> {
    private final CategoryRepository categories;
    private final CatalogReadPort catalog;
    private final ProductSearchIndex searchIndex;
    private final Clock clock;

    public DeleteCategoryCommandHandler(CategoryRepository categories, CatalogReadPort catalog,
                                        ProductSearchIndex searchIndex, Clock clock) {
        this.categories = categories;
        this.catalog = catalog;
        this.searchIndex = searchIndex;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> handle(DeleteCategoryCommand command) {
        Category category = categories.findById(new CategoryId(command.id())).orElse(null);
        if (category == null) {
            return Result.failure(new ApplicationError(
                    "CATEGORY_NOT_FOUND", "Category was not found", ErrorType.NOT_FOUND));
        }
        category.deactivate(clock.instant());
        categories.save(category);
        searchIndex.rebuild(catalog.findAllActiveProducts());
        return Result.success();
    }
}
