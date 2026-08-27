package com.johnvo.retailhub.application.features.catalog.command.createcategory;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CreateCategoryCommandHandler implements CommandHandler<CreateCategoryCommand, CreatedId> {
    private final CategoryRepository categories;
    private final Clock clock;

    public CreateCategoryCommandHandler(CategoryRepository categories, Clock clock) {
        this.categories = categories;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<CreatedId> handle(CreateCategoryCommand command) {
        if (categories.findByName(command.name()).isPresent()) {
            return Result.failure(new ApplicationError("CATEGORY_NAME_EXISTS",
                    "A category with this name already exists", ErrorType.CONFLICT));
        }
        try {
            Category saved = categories.save(Category.create(CategoryId.newId(), command.name(),
                    command.description(), clock.instant()));
            return Result.success(new CreatedId(saved.id().value()));
        } catch (DomainException exception) {
            return Result.failure(new ApplicationError(
                    "CATEGORY_INVALID", exception.getMessage(), ErrorType.VALIDATION));
        }
    }
}
