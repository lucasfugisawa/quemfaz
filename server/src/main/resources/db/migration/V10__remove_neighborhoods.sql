-- V10: Remove neighborhoods from the product (YAGNI for small cities)

-- Drop the neighborhoods join table
DROP TABLE IF EXISTS professional_profile_neighborhoods;

-- Drop the neighborhoods column from search_queries
ALTER TABLE search_queries DROP COLUMN IF EXISTS neighborhoods_json;
