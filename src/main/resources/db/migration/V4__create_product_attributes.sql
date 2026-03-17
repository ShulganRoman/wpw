CREATE TYPE rotation_direction_enum AS ENUM ('right', 'left', 'both');
CREATE TYPE bore_type_enum          AS ENUM ('shank', 'bore');
CREATE TYPE stock_status_enum       AS ENUM ('in_stock', 'low_stock', 'out_of_stock', 'on_order');

CREATE TABLE product_attributes (
    product_id         UUID PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    -- Геометрия
    d_mm               NUMERIC(7,3),
    d1_mm              NUMERIC(7,3),
    d2_mm              NUMERIC(7,3),
    b_mm               NUMERIC(7,3),
    b1_mm              NUMERIC(7,3),
    l_mm               NUMERIC(7,3),
    l1_mm              NUMERIC(7,3),
    r_mm               NUMERIC(7,3),
    a_mm               NUMERIC(7,3),
    angle_deg          NUMERIC(6,2),
    shank_mm           NUMERIC(7,3),
    shank_inch         VARCHAR(10),
    flutes             SMALLINT,
    blade_no           SMALLINT,
    cutting_type       VARCHAR(30),
    has_ball_bearing   BOOLEAN NOT NULL DEFAULT false,
    has_retainer       BOOLEAN NOT NULL DEFAULT false,
    can_resharpen      BOOLEAN NOT NULL DEFAULT false,
    rotation_direction rotation_direction_enum,
    bore_type          bore_type_enum,
    -- E-commerce
    ean13              VARCHAR(13),
    upc12              VARCHAR(12),
    custom_barcode     VARCHAR(50),
    hs_code            VARCHAR(15),
    country_of_origin  CHAR(2),
    weight_g           INT,
    weight_gross_g     INT,
    pkg_length_mm      SMALLINT,
    pkg_width_mm       SMALLINT,
    pkg_height_mm      SMALLINT,
    pkg_qty            SMALLINT NOT NULL DEFAULT 1,
    carton_qty         SMALLINT,
    -- Stock
    stock_qty          INT,
    stock_status       stock_status_enum,
    stock_updated_at   TIMESTAMPTZ
);

CREATE INDEX idx_pa_d_mm        ON product_attributes (d_mm);
CREATE INDEX idx_pa_shank_mm    ON product_attributes (shank_mm);
CREATE INDEX idx_pa_cutting_type ON product_attributes (cutting_type);
CREATE INDEX idx_pa_in_stock    ON product_attributes (stock_status) WHERE stock_status = 'in_stock';
