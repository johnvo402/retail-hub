package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepository {
    private final SpringDataCategoryRepository repository;

    public JpaCategoryRepositoryAdapter(SpringDataCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        return repository.findById(id.value()).map(JpaCategoryRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return repository.findByNameIgnoreCase(name).map(JpaCategoryRepositoryAdapter::toDomain);
    }

    @Override
    public List<Category> findAllActive() {
        return repository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(JpaCategoryRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Category save(Category category) {
        return toDomain(repository.save(new CategoryJpaEntity(category.id().value(), category.name(),
                category.description(), category.active(), category.createdAt(), category.updatedAt())));
    }

    static Category toDomain(CategoryJpaEntity entity) {
        return Category.reconstitute(new CategoryId(entity.getId()), entity.getName(), entity.getDescription(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

