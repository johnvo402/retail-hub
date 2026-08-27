package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataInventoryMovementRepository
        extends JpaRepository<InventoryMovementJpaEntity, UUID> {
    Page<InventoryMovementJpaEntity> findByProductId(UUID productId, Pageable pageable);

    long countByProductId(UUID productId);
}
