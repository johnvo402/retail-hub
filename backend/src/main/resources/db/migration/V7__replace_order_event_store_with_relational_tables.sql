CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_orders_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED'))
);

INSERT INTO orders (id, customer_id, status, created_at, updated_at, confirmed_at, cancelled_at, version)
SELECT orm.id,
       orm.customer_id,
       orm.status,
       orm.created_at,
       COALESCE((SELECT MAX(de.occurred_at) FROM domain_events de WHERE de.aggregate_id = orm.id),
                orm.created_at),
       orm.confirmed_at,
       orm.cancelled_at,
       0
FROM order_read_models orm;

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(80) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT ck_order_items_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

INSERT INTO order_items (id, order_id, product_id, product_name, sku, unit_price, quantity)
SELECT id, order_id, product_id, product_name, sku, unit_price, quantity
FROM order_item_read_models;

DROP TABLE order_item_read_models;
DROP TABLE order_read_models;
DROP TABLE domain_events;

CREATE INDEX ix_orders_customer_id ON orders (customer_id);
CREATE INDEX ix_orders_status ON orders (status);
CREATE INDEX ix_orders_created_at ON orders (created_at DESC);
CREATE INDEX ix_order_items_order_id ON order_items (order_id);
CREATE INDEX ix_order_items_product_id ON order_items (product_id);
