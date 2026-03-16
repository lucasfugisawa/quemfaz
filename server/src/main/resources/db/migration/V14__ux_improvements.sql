-- V14__ux_improvements.sql

-- 1. Merge first_name + last_name into full_name
ALTER TABLE users ADD COLUMN full_name TEXT NOT NULL DEFAULT '';
UPDATE users SET full_name = TRIM(CONCAT(first_name, ' ', last_name));
ALTER TABLE users DROP COLUMN first_name;
ALTER TABLE users DROP COLUMN last_name;

-- 2. Add date_of_birth to users
ALTER TABLE users ADD COLUMN date_of_birth DATE;

-- 3. Drop phone columns from professional_profiles
ALTER TABLE professional_profiles DROP COLUMN contact_phone;
ALTER TABLE professional_profiles DROP COLUMN whatsapp_phone;

-- 4. Create search_events table for popular searches
CREATE TABLE search_events (
    id TEXT PRIMARY KEY,
    resolved_service_id TEXT NOT NULL,
    city_name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_events_city_created ON search_events (city_name, created_at);
CREATE INDEX idx_search_events_created ON search_events (created_at);
