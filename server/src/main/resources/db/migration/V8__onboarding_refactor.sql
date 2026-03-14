-- V8__onboarding_refactor.sql

-- users: replace name column with first_name + last_name
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users ADD COLUMN first_name TEXT NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN last_name  TEXT NOT NULL DEFAULT '';

-- professional_profiles: add optional known_name
ALTER TABLE professional_profiles ADD COLUMN known_name TEXT;

-- stored_images: temporary blob storage for profile photos
CREATE TABLE stored_images (
    id           TEXT        PRIMARY KEY,
    data         BYTEA       NOT NULL,
    content_type TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
