CREATE TYPE file_type_enum AS ENUM ('image', 'drawing', 'video', 'document');

CREATE TABLE media_files (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id    UUID REFERENCES products(id) ON DELETE CASCADE,
    group_id      UUID REFERENCES product_groups(id) ON DELETE CASCADE,
    category_id   UUID REFERENCES categories(id) ON DELETE CASCADE,
    file_type     file_type_enum NOT NULL DEFAULT 'image',
    url           VARCHAR(1000) NOT NULL,
    thumbnail_url VARCHAR(1000),
    alt_text      VARCHAR(300),
    sort_order    INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_media_owner CHECK (
        (product_id IS NOT NULL)::int +
        (group_id IS NOT NULL)::int +
        (category_id IS NOT NULL)::int = 1
    )
);

CREATE INDEX idx_media_product_id  ON media_files (product_id)  WHERE product_id  IS NOT NULL;
CREATE INDEX idx_media_group_id    ON media_files (group_id)    WHERE group_id    IS NOT NULL;
CREATE INDEX idx_media_category_id ON media_files (category_id) WHERE category_id IS NOT NULL;
