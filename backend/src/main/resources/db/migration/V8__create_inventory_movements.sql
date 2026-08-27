CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    movement_type VARCHAR(40) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    quantity_before INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reference_id UUID,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_inventory_movements_type CHECK (
        movement_type IN ('MANUAL_INCREASE', 'MANUAL_DECREASE', 'ORDER_CONFIRMATION')
    ),
    CONSTRAINT ck_inventory_movements_delta CHECK (quantity_delta <> 0),
    CONSTRAINT ck_inventory_movements_before CHECK (quantity_before >= 0),
    CONSTRAINT ck_inventory_movements_after CHECK (quantity_after >= 0),
    CONSTRAINT ck_inventory_movements_balance CHECK (
        quantity_after::BIGINT = quantity_before::BIGINT + quantity_delta::BIGINT
    ),
    CONSTRAINT ck_inventory_movements_direction CHECK (
        (movement_type = 'MANUAL_INCREASE' AND quantity_delta > 0)
        OR (movement_type IN ('MANUAL_DECREASE', 'ORDER_CONFIRMATION') AND quantity_delta < 0)
    )
);

CREATE INDEX ix_inventory_movements_product_created_at
    ON inventory_movements (product_id, created_at DESC);
