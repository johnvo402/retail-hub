package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    Optional<CategoryJpaEntity> findByNameIgnoreCase(String name);

    List<CategoryJpaEntity> findAllByActiveTrueOrderByNameAsc();
}

