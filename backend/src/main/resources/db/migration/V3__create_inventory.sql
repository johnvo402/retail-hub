CREATE TABLE inventory_items (
    product_id UUID PRIMARY KEY REFERENCES products(id),
    quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_inventory_quantity CHECK (quantity >= 0)
);

CREATE INDEX ix_inventory_updated_at ON inventory_items (updated_at DESC);

