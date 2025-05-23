
CREATE TABLE items (
    id UUID PRIMARY KEY,
    merchant_id TEXT NOT NULL,
    product_id UUID NOT NULL,
    brand_id UUID NOT NULL,
    metadata JSONB
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_ids UUID[] NOT NULL,
    metadata JSONB
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    product_name TEXT NOT NULL,
    brand_id UUID NOT NULL,
    images TEXT[]
);

CREATE TABLE brands (
    id UUID PRIMARY KEY,
    brand_name TEXT NOT NULL,
    images TEXT[]
);
