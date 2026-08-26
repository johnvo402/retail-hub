package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryRepository extends JpaRepository<InventoryJpaEntity, UUID> {
    @EntityGraph(attributePaths = "product")
    @Query("select i from InventoryJpaEntity i where i.productId = :productId")
    Optional<InventoryJpaEntity> findDetailedByProductId(@Param("productId") UUID productId);

    @Override
    @EntityGraph(attributePaths = "product")
    Page<InventoryJpaEntity> findAll(Pageable pageable);
}

