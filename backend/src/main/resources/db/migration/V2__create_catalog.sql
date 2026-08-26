CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_categories_name ON categories (LOWER(name));
CREATE INDEX ix_categories_active ON categories (active);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES categories(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    sku VARCHAR(80) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_products_price CHECK (price >= 0)
);

CREATE UNIQUE INDEX ux_products_sku ON products (UPPER(sku));
CREATE INDEX ix_products_category_id ON products (category_id);
CREATE INDEX ix_products_active ON products (active);
CREATE INDEX ix_products_created_at ON products (created_at DESC);

