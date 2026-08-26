package com.johnvo.retailhub.domain.catalog;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(CategoryId id);

    Optional<Category> findByName(String name);

    List<Category> findAllActive();

    Category save(Category category);
}

