package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    @EntityGraph(attributePaths = "items")
    @Query("select o from OrderJpaEntity o where o.id = :id")
    Optional<OrderJpaEntity> findDetailedById(@Param("id") UUID id);

    @Query(value = """
            SELECT o.id AS id,
                   o.customer_id AS customerId,
                   o.status AS status,
                   COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS totalAmount,
                   COALESCE(SUM(oi.quantity), 0) AS itemCount,
                   o.created_at AS createdAt,
                   o.confirmed_at AS confirmedAt,
                   o.cancelled_at AS cancelledAt
            FROM orders o
            LEFT JOIN order_items oi ON oi.order_id = o.id
            GROUP BY o.id
            ORDER BY o.created_at DESC
            """, countQuery = "SELECT COUNT(*) FROM orders", nativeQuery = true)
    Page<OrderSummaryProjection> findAllSummaries(Pageable pageable);

    @Query(value = """
            SELECT o.id AS id,
                   o.customer_id AS customerId,
                   o.status AS status,
                   COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS totalAmount,
                   COALESCE(SUM(oi.quantity), 0) AS itemCount,
                   o.created_at AS createdAt,
                   o.confirmed_at AS confirmedAt,
                   o.cancelled_at AS cancelledAt
            FROM orders o
            LEFT JOIN order_items oi ON oi.order_id = o.id
            WHERE o.customer_id = :customerId
            GROUP BY o.id
            ORDER BY o.created_at DESC
            """, countQuery = "SELECT COUNT(*) FROM orders WHERE customer_id = :customerId",
            nativeQuery = true)
    Page<OrderSummaryProjection> findAllSummariesByCustomerId(@Param("customerId") UUID customerId,
                                                               Pageable pageable);
}
