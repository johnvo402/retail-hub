package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderReadRepository extends JpaRepository<OrderReadModelJpaEntity, UUID> {
    @EntityGraph(attributePaths = "items")
    @Query("select o from OrderReadModelJpaEntity o where o.id = :id")
    Optional<OrderReadModelJpaEntity> findDetailedById(@Param("id") UUID id);

    Page<OrderReadModelJpaEntity> findAllByCustomerId(UUID customerId, Pageable pageable);
}

