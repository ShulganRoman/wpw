-- V18: Recreate dealer_sku_mapping with text-based PK (dealer_id, wpw_sku)

DROP TABLE IF EXISTS dealer_sku_mapping;

CREATE TABLE dealer_sku_mapping
(
    dealer_id    UUID NOT NULL REFERENCES dealers (id) ON DELETE CASCADE,
    wpw_sku      TEXT NOT NULL,
    dealer_sku   TEXT NOT NULL,
    dealer_brand TEXT,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (dealer_id, wpw_sku)
);

CREATE INDEX idx_sku_mapping_dealer ON dealer_sku_mapping (dealer_id);
