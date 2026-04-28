-- V16: Add full dealer management schema

-- Extend dealers table with new fields
ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS dealer_code        TEXT UNIQUE,
    ADD COLUMN IF NOT EXISTS company_name       TEXT,
    ADD COLUMN IF NOT EXISTS brand_name         TEXT,
    ADD COLUMN IF NOT EXISTS dealer_type        TEXT,
    ADD COLUMN IF NOT EXISTS private_label_brand TEXT,
    ADD COLUMN IF NOT EXISTS region             TEXT,
    ADD COLUMN IF NOT EXISTS city               TEXT,
    ADD COLUMN IF NOT EXISTS address            TEXT,
    ADD COLUMN IF NOT EXISTS postal_code        TEXT,
    ADD COLUMN IF NOT EXISTS website            TEXT,
    ADD COLUMN IF NOT EXISTS has_ecommerce      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS shop_url           TEXT,
    ADD COLUMN IF NOT EXISTS logo               TEXT,
    ADD COLUMN IF NOT EXISTS currency           TEXT,
    ADD COLUMN IF NOT EXISTS discount_tier      TEXT,
    ADD COLUMN IF NOT EXISTS notes              TEXT,
    ADD COLUMN IF NOT EXISTS updated_at         TIMESTAMPTZ DEFAULT NOW();

-- Change country from CHAR(2) to TEXT to store full country name
ALTER TABLE dealers ALTER COLUMN country TYPE TEXT;

-- Dealer contacts table
CREATE TABLE dealer_contacts
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dealer_id    UUID NOT NULL REFERENCES dealers (id) ON DELETE CASCADE,
    contact_name TEXT NOT NULL,
    role         TEXT,
    email        TEXT,
    phone        TEXT,
    is_primary   BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ      DEFAULT NOW()
);

CREATE INDEX idx_dealer_contacts_dealer ON dealer_contacts (dealer_id);
CREATE INDEX idx_dealers_dealer_code ON dealers (dealer_code);

-- Extend dealer_sku_mapping with WPW SKU string and brand
ALTER TABLE dealer_sku_mapping
    ADD COLUMN IF NOT EXISTS wpw_sku      TEXT,
    ADD COLUMN IF NOT EXISTS dealer_brand TEXT;
