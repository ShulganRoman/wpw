ALTER TABLE operations ADD COLUMN name VARCHAR(100);

UPDATE operations SET name = 'Nesting'            WHERE code = 'nesting';
UPDATE operations SET name = 'Profiling'           WHERE code = 'profiling';
UPDATE operations SET name = 'Grooving'            WHERE code = 'grooving';
UPDATE operations SET name = 'Trimming'            WHERE code = 'trimming';
UPDATE operations SET name = 'Jointing'            WHERE code = 'jointing';
UPDATE operations SET name = 'Drilling'            WHERE code = 'drilling';
UPDATE operations SET name = 'Surface Processing'  WHERE code = 'surface';

-- Fill any remaining rows that might not match
UPDATE operations SET name = initcap(code) WHERE name IS NULL;

ALTER TABLE operations ALTER COLUMN name SET NOT NULL;
