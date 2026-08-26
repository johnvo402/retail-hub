package com.johnvo.retailhub.infrastructure.eventstore;

import com.johnvo.retailhub.application.common.ConflictException;
import com.johnvo.retailhub.application.features.ordering.common.OrderEventStore;
import com.johnvo.retailhub.domain.ordering.events.OrderEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class PostgresOrderEventStore implements OrderEventStore {
    private final SpringDataDomainEventRepository repository;
    private final OrderEventSerializer serializer;

    public PostgresOrderEventStore(SpringDataDomainEventRepository repository,
                                   OrderEventSerializer serializer) {
        this.repository = repository;
        this.serializer = serializer;
    }

    @Override
    public List<OrderEvent> load(UUID orderId) {
        return repository.findByAggregateIdOrderByVersionAsc(orderId).stream()
                .map(entity -> serializer.deserialize(entity.getEventType(), entity.getPayload()))
                .toList();
    }

    @Override
    public void append(UUID orderId, long expectedVersion, List<OrderEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        List<DomainEventJpaEntity> entities = new ArrayList<>(events.size());
        long version = expectedVersion;
        for (OrderEvent event : events) {
            version++;
            entities.add(new DomainEventJpaEntity(UUID.randomUUID(), orderId, "Order", version,
                    event.getClass().getSimpleName(), serializer.serialize(event), event.occurredAt()));
        }
        try {
            repository.saveAllAndFlush(entities);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Order was modified by another request; reload and retry", exception);
        }
    }
}

