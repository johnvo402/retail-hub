CREATE TABLE order_read_models (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    item_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT ck_order_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_order_total CHECK (total_amount >= 0),
    CONSTRAINT ck_order_item_count CHECK (item_count >= 0)
);

CREATE INDEX ix_orders_customer_id ON order_read_models (customer_id);
CREATE INDEX ix_orders_status ON order_read_models (status);
CREATE INDEX ix_orders_created_at ON order_read_models (created_at DESC);

CREATE TABLE order_item_read_models (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES order_read_models(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(80) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT ck_order_item_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_item_total CHECK (line_total >= 0)
);

CREATE INDEX ix_order_items_order_id ON order_item_read_models (order_id);
CREATE INDEX ix_order_items_product_id ON order_item_read_models (product_id);

