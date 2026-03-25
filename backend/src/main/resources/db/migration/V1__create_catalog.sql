CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE sections (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug         VARCHAR(100) UNIQUE NOT NULL,
    translations JSONB NOT NULL DEFAULT '{}',
    sort_order   INT NOT NULL DEFAULT 0,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE categories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id   UUID NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    slug         VARCHAR(100) UNIQUE NOT NULL,
    translations JSONB NOT NULL DEFAULT '{}',
    sort_order   INT NOT NULL DEFAULT 0,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE product_groups (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id  UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    slug         VARCHAR(100) UNIQUE NOT NULL,
    group_code   VARCHAR(50),
    translations JSONB NOT NULL DEFAULT '{}',
    sort_order   INT NOT NULL DEFAULT 0,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sections_translations     ON sections      USING GIN (translations);
CREATE INDEX idx_categories_translations   ON categories    USING GIN (translations);
CREATE INDEX idx_product_groups_translations ON product_groups USING GIN (translations);
CREATE INDEX idx_categories_section_id     ON categories    (section_id);
CREATE INDEX idx_product_groups_category_id ON product_groups (category_id);
