------------------------------------------------------------
-- Drop the stale city_name column from professional_profiles.
-- Data was backfilled to city_id in V3; city_name is no longer referenced.
------------------------------------------------------------

DROP INDEX IF EXISTS idx_professional_profiles_city_name;
ALTER TABLE professional_profiles DROP COLUMN IF EXISTS city_name;
