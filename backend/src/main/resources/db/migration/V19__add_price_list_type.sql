ALTER TABLE price_lists
    ADD COLUMN IF NOT EXISTS type VARCHAR(10) NOT NULL DEFAULT 'dealer';

CREATE INDEX IF NOT EXISTS idx_price_lists_type ON price_lists(type);
