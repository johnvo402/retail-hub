package com.johnvo.retailhub.application.features.catalog.command.createcategory;

import com.johnvo.retailhub.application.common.ConflictException;
import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
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
    public CreatedId handle(CreateCategoryCommand command) {
        if (categories.findByName(command.name()).isPresent()) {
            throw new ConflictException("A category with this name already exists");
        }
        Category saved = categories.save(Category.create(CategoryId.newId(), command.name(),
                command.description(), clock.instant()));
        return new CreatedId(saved.id().value());
    }
}

