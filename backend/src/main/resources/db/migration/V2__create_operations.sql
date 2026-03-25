CREATE TABLE operations (
    code       VARCHAR(30) PRIMARY KEY,
    name_key   VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

INSERT INTO operations (code, name_key, sort_order) VALUES
    ('nesting',   'op.nesting',   1),
    ('profiling', 'op.profiling', 2),
    ('grooving',  'op.grooving',  3),
    ('trimming',  'op.trimming',  4),
    ('jointing',  'op.jointing',  5),
    ('drilling',  'op.drilling',  6),
    ('surface',   'op.surface',   7);
