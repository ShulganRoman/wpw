CREATE TABLE currencies
(
    code           CHAR(3) PRIMARY KEY,
    name           VARCHAR(50) NOT NULL,
    symbol         VARCHAR(5)  NOT NULL,
    decimal_places SMALLINT    NOT NULL DEFAULT 2,
    is_active      BOOLEAN     NOT NULL DEFAULT true
);

INSERT INTO currencies (code, name, symbol)
VALUES ('ILS', 'Israeli New Shekel', '₪'),
       ('USD', 'US Dollar', '$'),
       ('EUR', 'Euro', '€'),
       ('GBP', 'British Pound', '£'),
       ('PLN', 'Polish Zloty', 'zł');

CREATE TABLE price_lists
(
    id            UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    name          VARCHAR(100)  NOT NULL,
    currency_code CHAR(3)       NOT NULL REFERENCES currencies (code),
    valid_from    DATE,
    valid_to      DATE,
    discount_pct  NUMERIC(5, 2) NOT NULL DEFAULT 0,
    notes         TEXT,
    is_active     BOOLEAN       NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ            DEFAULT NOW()
);

CREATE TABLE price_list_items
(
    price_list_id UUID           NOT NULL REFERENCES price_lists (id) ON DELETE CASCADE,
    product_id    UUID           NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    min_qty       INT            NOT NULL DEFAULT 1,
    price         NUMERIC(12, 4) NOT NULL,
    PRIMARY KEY (price_list_id, product_id, min_qty)
);

CREATE TABLE dealers
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name           VARCHAR(200) NOT NULL,
    country        CHAR(2),
    default_locale VARCHAR(10)  NOT NULL DEFAULT 'en',
    api_key_hash   VARCHAR(60)  NOT NULL,
    webhook_url    VARCHAR(500),
    price_list_id  UUID REFERENCES price_lists (id),
    is_active      BOOLEAN      NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ           DEFAULT NOW()
);

CREATE TABLE dealer_sku_mapping
(
    dealer_id  UUID         NOT NULL REFERENCES dealers (id) ON DELETE CASCADE,
    product_id UUID         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    dealer_sku VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (dealer_id, product_id)
);

CREATE INDEX idx_price_list_items_pl ON price_list_items (price_list_id);
CREATE INDEX idx_dealer_sku_dealer ON dealer_sku_mapping (dealer_id);
