package com.johnvo.retailhub.infrastructure.eventstore;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "domain_events", uniqueConstraints =
        @UniqueConstraint(name = "ux_domain_events_aggregate_version", columnNames = {"aggregate_id", "version"}))
public class DomainEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false)
    private long version;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected DomainEventJpaEntity() {
    }

    public DomainEventJpaEntity(UUID id, UUID aggregateId, String aggregateType, long version,
                                String eventType, JsonNode payload, Instant occurredAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.version = version;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public UUID getAggregateId() { return aggregateId; }
    public long getVersion() { return version; }
    public String getEventType() { return eventType; }
    public JsonNode getPayload() { return payload; }
}

