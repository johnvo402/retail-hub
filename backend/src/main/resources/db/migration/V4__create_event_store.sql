CREATE TABLE domain_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_domain_events_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX ix_domain_events_aggregate ON domain_events (aggregate_id, version);
CREATE INDEX ix_domain_events_occurred_at ON domain_events (occurred_at);

