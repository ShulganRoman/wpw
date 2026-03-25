CREATE TYPE product_type_enum   AS ENUM ('main', 'spare_part', 'accessory');
CREATE TYPE product_status_enum AS ENUM ('active', 'discontinued', 'coming_soon');

CREATE TABLE products (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tool_no      VARCHAR(50) UNIQUE NOT NULL,
    alt_tool_no  VARCHAR(50),
    group_id     UUID REFERENCES product_groups(id) ON DELETE SET NULL,
    product_type product_type_enum  NOT NULL DEFAULT 'main',
    status       product_status_enum NOT NULL DEFAULT 'active',
    is_orderable BOOLEAN NOT NULL DEFAULT true,
    catalog_page SMALLINT,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE product_translations (
    product_id        UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    locale            VARCHAR(10) NOT NULL,
    name              VARCHAR(500) NOT NULL,
    short_description TEXT,
    long_description  TEXT,
    seo_title         VARCHAR(70),
    seo_description   VARCHAR(170),
    ai_generated      BOOLEAN NOT NULL DEFAULT false,
    reviewed_at       TIMESTAMPTZ,
    PRIMARY KEY (product_id, locale)
);

CREATE INDEX idx_products_tool_no  ON products (tool_no);
CREATE INDEX idx_products_group_id ON products (group_id);
CREATE INDEX idx_products_status   ON products (status);
CREATE INDEX idx_products_active   ON products (id) WHERE status = 'active';

CREATE INDEX idx_pt_fts ON product_translations
    USING GIN (to_tsvector('simple',
        name || ' ' || COALESCE(short_description, '') || ' ' || COALESCE(long_description, '')));
