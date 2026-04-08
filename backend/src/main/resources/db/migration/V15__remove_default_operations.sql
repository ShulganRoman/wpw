-- Remove the hardcoded default operation tags and their product assignments.
-- Application tags are now managed exclusively through the admin UI and Excel import.

DELETE FROM product_operations
WHERE operation_code IN ('nesting','profiling','grooving','trimming','jointing','drilling','surface');

DELETE FROM operations
WHERE code IN ('nesting','profiling','grooving','trimming','jointing','drilling','surface');
