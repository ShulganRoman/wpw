ALTER TABLE product_attributes
    ADD COLUMN ball_bearing_code VARCHAR(100),
    ADD COLUMN retainer_code     VARCHAR(50);

ALTER TABLE product_translations
    ADD COLUMN applications TEXT;
