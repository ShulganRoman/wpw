-- Assign operation codes to products based on their product group names/slugs
-- Mapping from specification table (Image #2):
--   nesting   → Compression spiral, Up/Down bits
--   profiling → Roundover, Ogee, Cove, Chamfer
--   grooving  → Dovetail, T-slot, Rabbet
--   trimming  → Flush trim, Template, Bevel trim
--   jointing  → Jointing bits, Edge trim
--   drilling  → Countersinks, Brad point, Industrial
--   surface   → Surfacing bits, Spoilboard cutters

-- nesting: Compression spiral, Up/Down bits
INSERT INTO product_operations (product_id, operation_code)
SELECT DISTINCT p.id, 'nesting'
FROM products p
JOIN product_groups pg ON p.group_id = pg.id
WHERE (
    LOWER(pg.translations::text) LIKE '%compression%'
    OR LOWER(pg.translations::text) LIKE '%up/down%'
    OR LOWER(pg.translations::text) LIKE '%up down%'
    OR LOWER(pg.translations::text) LIKE '%updown%'
    OR LOWER(pg.slug) LIKE '%compression%'
    OR LOWER(pg.slug) LIKE '%up-down%'
    OR LOWER(pg.slug) LIKE '%updown%'
    OR LOWER(pg.group_code) LIKE '%compression%'
    OR LOWER(pg.group_code) LIKE '%up-down%'
    OR LOWER(pg.group_code) LIKE '%nesting%'
)
AND NOT EXISTS (
    SELECT 1 FROM product_operations po WHERE po.product_id = p.id AND po.operation_code = 'nesting'
);

-- profiling: Roundover, Ogee, Cove, Chamfer
INSERT INTO product_operations (product_id, operation_code)
SELECT DISTINCT p.id, 'profiling'
FROM products p
JOIN product_groups pg ON p.group_id = pg.id
WHERE (
    LOWER(pg.translations::text) LIKE '%roundover%'
    OR LOWER(pg.translations::text) LIKE '%round over%'
    OR LOWER(pg.translations::text) LIKE '%ogee%'
    OR LOWER(pg.translations::text) LIKE '%cove%'
    OR LOWER(pg.translations::text) LIKE '%chamfer%'
    OR LOWER(pg.slug) LIKE '%roundover%'
    OR LOWER(pg.slug) LIKE '%ogee%'
    OR LOWER(pg.slug) LIKE '%cove%'
    OR LOWER(pg.slug) LIKE '%chamfer%'
    OR LOWER(pg.slug) LIKE '%profiling%'
    OR LOWER(pg.group_code) LIKE '%roundover%'
    OR LOWER(pg.group_code) LIKE '%ogee%'
    OR LOWER(pg.group_code) LIKE '%cove%'
    OR LOWER(pg.group_code) LIKE '%chamfer%'
    OR LOWER(pg.group_code) LIKE '%profiling%'
)
AND NOT EXISTS (
    SELECT 1 FROM product_operations po WHERE po.product_id = p.id AND po.operation_code = 'profiling'
);

-- grooving: Dovetail, T-slot, Rabbet
INSERT INTO product_operations (product_id, operation_code)
SELECT DISTINCT p.id, 'grooving'
FROM products p
JOIN product_groups pg ON p.group_id = pg.id
WHERE (
    LOWER(pg.translations::text) LIKE '%dovetail%'
    OR LOWER(pg.translations::text) LIKE '%t-slot%'
    OR LOWER(pg.translations::text) LIKE '%t slot%'
    OR LOWER(pg.translations::text) LIKE '%rabbet%'
    OR LOWER(pg.translations::text) LIKE '%dado%'
    OR LOWER(pg.translations::text) LIKE '%groove%'
    OR LOWER(pg.translations::text) LIKE '%grooving%'
    OR LOWER(pg.slug) LIKE '%dovetail%'
    OR LOWER(pg.slug) LIKE '%t-slot%'
    OR LOWER(pg.slug) LIKE '%rabbet%'
    OR LOWER(pg.slug) LIKE '%groove%'
    OR LOWER(pg.slug) LIKE '%grooving%'
    OR LOWER(pg.group_code) LIKE '%dovetail%'
    OR LOWER(pg.group_code) LIKE '%t-slot%'
    OR LOWER(pg.group_code) LIKE '%rabbet%'
    OR LOWER(pg.group_code) LIKE '%groove%'
    OR LOWER(pg.group_code) LIKE '%grooving%'
)
AND NOT EXISTS (
    SELECT 1 FROM product_operations po WHERE po.product_id = p.id AND po.operation_code = 'grooving'
);

-- trimming: Flush trim, Template, Bevel trim
INSERT INTO product_operations (product_id, operation_code)
SELECT DISTINCT p.id, 'trimming'
FROM products p
JOIN product_groups pg ON p.group_id = pg.id
WHERE (
    LOWER(pg.translations::text) LIKE '%flush trim%'
    OR LOWER(pg.translations::text) LIKE '%flush-trim%'
    OR LOWER(pg.translations::text) LIKE '%template%'
    OR LOWER(pg.translations::text) LIKE '%bevel trim%'
    OR LOWER(pg.translations::text) LIKE '%bevel-trim%'
    OR LOWER(pg.translations::text) LIKE '%trimming%'
    OR LOWER(pg.slug) LIKE '%flush%trim%'
    OR LOWER(pg.slug) LIKE '%template%'
    OR LOWER(pg.slug) LIKE '%bevel%trim%'
    OR LOWER(pg.slug) LIKE '%trimming%'
    OR LOWER(pg.group_code) LIKE '%flush%trim%'
    OR LOWER(pg.group_code) LIKE '%template%'
    OR LOWER(pg.group_code) LIKE '%bevel%trim%'
    OR LOWER(pg.group_code) LIKE '%trimming%'
)
AND NOT EXISTS (
    SELECT 1 FROM product_operations po WHERE po.product_id = p.id AND po.operation_code = 'trimming'
);

-- jointing: Jointing bits, Edge trim
INSERT INTO product_operations (product_id, operation_code)
SELECT DISTINCT p.id, 'jointing'
FROM products p
JOIN product_groups pg ON p.group_id = pg.id
WHERE (
    LOWER(pg.translations::text) LIKE '%jointing%'
    OR LOWER(pg.translations::text) LIKE '%edge trim%'
    OR LOWER(pg.translations::text) LIKE '%edge-trim%'
    OR LOWER(pg.translations::text) LIKE '%edge band%'
    OR LOWER(pg.translations::text) LIKE '%edgebanding%'
    OR LOWER(pg.slug) LIKE '%jointing%'
    OR LOWER(pg.slug) LIKE '%edge%trim%'
    OR LOWER(pg.slug) LIKE '%edge%band%'
    OR LOWER(pg.group_code) LIKE '%jointing%'
    OR LOWER(pg.group_code) LIKE '%edge%trim%'
    OR LOWER(pg.group_code) LIKE '%edge%band%'
)
AND NOT EXISTS (
    SELECT 1 FROM product_operations po WHERE po.product_id = p.id AND po.operation_code = 'jointing'
);

-- drilling: Countersinks, Brad point, Industrial
INSERT INTO product_operations (product_id, operation_code)
SELECT DISTINCT p.id, 'drilling'
FROM products p
JOIN product_groups pg ON p.group_id = pg.id
WHERE (
    LOWER(pg.translations::text) LIKE '%countersink%'
    OR LOWER(pg.translations::text) LIKE '%brad point%'
    OR LOWER(pg.translations::text) LIKE '%brad-point%'
    OR LOWER(pg.translations::text) LIKE '%industrial drill%'
    OR LOWER(pg.translations::text) LIKE '%drill%'
    OR LOWER(pg.translations::text) LIKE '%boring%'
    OR LOWER(pg.slug) LIKE '%countersink%'
    OR LOWER(pg.slug) LIKE '%brad%point%'
    OR LOWER(pg.slug) LIKE '%drill%'
    OR LOWER(pg.slug) LIKE '%boring%'
    OR LOWER(pg.group_code) LIKE '%countersink%'
    OR LOWER(pg.group_code) LIKE '%brad%point%'
    OR LOWER(pg.group_code) LIKE '%drill%'
    OR LOWER(pg.group_code) LIKE '%boring%'
)
AND NOT EXISTS (
    SELECT 1 FROM product_operations po WHERE po.product_id = p.id AND po.operation_code = 'drilling'
);

-- surface: Surfacing bits, Spoilboard cutters
INSERT INTO product_operations (product_id, operation_code)
SELECT DISTINCT p.id, 'surface'
FROM products p
JOIN product_groups pg ON p.group_id = pg.id
WHERE (
    LOWER(pg.translations::text) LIKE '%surfacing%'
    OR LOWER(pg.translations::text) LIKE '%spoilboard%'
    OR LOWER(pg.translations::text) LIKE '%surface routing%'
    OR LOWER(pg.translations::text) LIKE '%planing%'
    OR LOWER(pg.slug) LIKE '%surfacing%'
    OR LOWER(pg.slug) LIKE '%spoilboard%'
    OR LOWER(pg.slug) LIKE '%surface%'
    OR LOWER(pg.group_code) LIKE '%surfacing%'
    OR LOWER(pg.group_code) LIKE '%spoilboard%'
    OR LOWER(pg.group_code) LIKE '%surface%'
)
AND NOT EXISTS (
    SELECT 1 FROM product_operations po WHERE po.product_id = p.id AND po.operation_code = 'surface'
);
