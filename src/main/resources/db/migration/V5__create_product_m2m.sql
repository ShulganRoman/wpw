CREATE TABLE product_tool_material (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    material   VARCHAR(30) NOT NULL,
    PRIMARY KEY (product_id, material)
);

CREATE TABLE product_workpiece_material (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    material   VARCHAR(30) NOT NULL,
    PRIMARY KEY (product_id, material)
);

CREATE TABLE product_machine_type (
    product_id   UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    machine_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (product_id, machine_type)
);

CREATE TABLE product_machine_brand (
    product_id    UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    machine_brand VARCHAR(30) NOT NULL,
    PRIMARY KEY (product_id, machine_brand)
);

CREATE TABLE product_operations (
    product_id     UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    operation_code VARCHAR(30) NOT NULL REFERENCES operations(code) ON DELETE CASCADE,
    PRIMARY KEY (product_id, operation_code)
);

CREATE TABLE product_relations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    to_product_id   UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    relation_type   VARCHAR(20) NOT NULL,
    UNIQUE (from_product_id, to_product_id, relation_type)
);

CREATE TABLE product_spare_parts (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    part_id    UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    part_role  VARCHAR(20) NOT NULL,
    PRIMARY KEY (product_id, part_id)
);

CREATE INDEX idx_ptm_product  ON product_tool_material (product_id);
CREATE INDEX idx_pwm_product  ON product_workpiece_material (product_id);
CREATE INDEX idx_pmt_product  ON product_machine_type (product_id);
CREATE INDEX idx_pmb_product  ON product_machine_brand (product_id);
CREATE INDEX idx_po_product   ON product_operations (product_id);
CREATE INDEX idx_pr_from      ON product_relations (from_product_id);
CREATE INDEX idx_psp_product  ON product_spare_parts (product_id);
CREATE INDEX idx_psp_part     ON product_spare_parts (part_id);
