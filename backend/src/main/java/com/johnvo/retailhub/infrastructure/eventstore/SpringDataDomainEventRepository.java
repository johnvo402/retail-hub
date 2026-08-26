package com.johnvo.retailhub.infrastructure.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDomainEventRepository extends JpaRepository<DomainEventJpaEntity, UUID> {
    List<DomainEventJpaEntity> findByAggregateIdOrderByVersionAsc(UUID aggregateId);
}

