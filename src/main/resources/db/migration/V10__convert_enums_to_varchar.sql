-- Hibernate 6 использует VARCHAR для @Enumerated(EnumType.STRING).
-- Конвертируем PostgreSQL native enum → VARCHAR.
-- Шаги: сначала удаляем partial indexes ссылающиеся на enum-значения, затем меняем тип, создаём индексы заново.

-- 1. Удаляем partial indexes с enum-условиями
DROP INDEX IF EXISTS idx_products_active;
DROP INDEX IF EXISTS idx_pa_in_stock;

-- 2. Меняем тип колонок: ::text — универсальный cast для любого PostgreSQL типа
ALTER TABLE products
    ALTER COLUMN product_type DROP DEFAULT,
    ALTER COLUMN status       DROP DEFAULT;

ALTER TABLE products
    ALTER COLUMN product_type TYPE VARCHAR(30) USING product_type::text,
    ALTER COLUMN status       TYPE VARCHAR(30) USING status::text;

ALTER TABLE products
    ALTER COLUMN product_type SET DEFAULT 'main',
    ALTER COLUMN status       SET DEFAULT 'active';

-- product_attributes
ALTER TABLE product_attributes
    ALTER COLUMN rotation_direction TYPE VARCHAR(20) USING rotation_direction::text,
    ALTER COLUMN bore_type          TYPE VARCHAR(20) USING bore_type::text,
    ALTER COLUMN stock_status       TYPE VARCHAR(30) USING stock_status::text;

-- media_files
ALTER TABLE media_files
    ALTER COLUMN file_type DROP DEFAULT;
ALTER TABLE media_files
    ALTER COLUMN file_type TYPE VARCHAR(20) USING file_type::text;
ALTER TABLE media_files
    ALTER COLUMN file_type SET DEFAULT 'image';

-- users
ALTER TABLE users
    ALTER COLUMN role DROP DEFAULT;
ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(20) USING role::text;
ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'viewer';

-- 3. Воссоздаём partial indexes
CREATE INDEX idx_products_active ON products (id) WHERE status = 'active';
CREATE INDEX idx_pa_in_stock ON product_attributes (stock_status) WHERE stock_status = 'in_stock';

-- 4. Удаляем неиспользуемые типы
DROP TYPE IF EXISTS product_type_enum;
DROP TYPE IF EXISTS product_status_enum;
DROP TYPE IF EXISTS rotation_direction_enum;
DROP TYPE IF EXISTS bore_type_enum;
DROP TYPE IF EXISTS stock_status_enum;
DROP TYPE IF EXISTS file_type_enum;
DROP TYPE IF EXISTS user_role_enum;
