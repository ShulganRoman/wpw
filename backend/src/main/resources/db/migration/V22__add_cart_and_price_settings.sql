-- V22: Cart for dealers + price visibility settings

ALTER TABLE system_settings
    ADD COLUMN require_price_admin  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN require_price_dealer BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN require_price_public BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE cart_items (
    dealer_id  UUID        NOT NULL REFERENCES dealers(id)  ON DELETE CASCADE,
    product_id UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    qty        INT         NOT NULL DEFAULT 1 CHECK (qty > 0),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (dealer_id, product_id)
);

CREATE INDEX idx_cart_items_dealer ON cart_items(dealer_id);
