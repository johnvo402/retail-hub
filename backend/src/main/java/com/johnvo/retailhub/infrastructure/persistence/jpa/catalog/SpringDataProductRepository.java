package com.johnvo.retailhub.infrastructure.persistence.jpa.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProductRepository
        extends JpaRepository<ProductJpaEntity, UUID>, JpaSpecificationExecutor<ProductJpaEntity> {
    Optional<ProductJpaEntity> findBySkuIgnoreCase(String sku);

    @EntityGraph(attributePaths = "category")
    @Query("select p from ProductJpaEntity p where p.id = :id")
    Optional<ProductJpaEntity> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "category")
    List<ProductJpaEntity> findAllByActiveTrueOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "category")
    Page<ProductJpaEntity> findAll(org.springframework.data.jpa.domain.Specification<ProductJpaEntity> spec,
                                   Pageable pageable);
}

