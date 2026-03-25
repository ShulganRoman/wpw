-- Hibernate 6 maps String to VARCHAR; PostgreSQL CHAR(n) = bpchar causes schema validation mismatch
ALTER TABLE currencies ALTER COLUMN code TYPE VARCHAR(3);
ALTER TABLE price_lists ALTER COLUMN currency_code TYPE VARCHAR(3);
ALTER TABLE dealers ALTER COLUMN country TYPE VARCHAR(2);
ALTER TABLE product_attributes ALTER COLUMN country_of_origin TYPE VARCHAR(2);
