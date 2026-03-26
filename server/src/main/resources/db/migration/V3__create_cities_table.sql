------------------------------------------------------------
-- Cities reference table
------------------------------------------------------------

CREATE TABLE cities (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    state_code TEXT NOT NULL,
    country TEXT NOT NULL DEFAULT 'BR',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

------------------------------------------------------------
-- Seed: initial supported cities
------------------------------------------------------------

INSERT INTO cities (id, name, state_code, latitude, longitude) VALUES
    ('batatais',        'Batatais',        'SP', -20.8914, -47.5864),
    ('franca',          'Franca',           'SP', -20.5389, -47.4008),
    ('ribeirao-preto',  'Ribeirão Preto',  'SP', -21.1767, -47.8208);

------------------------------------------------------------
-- Add city_id FK to professional_profiles
------------------------------------------------------------

ALTER TABLE professional_profiles ADD COLUMN city_id TEXT REFERENCES cities(id);

-- Backfill city_id from existing city_name values
UPDATE professional_profiles SET city_id = 'batatais'       WHERE lower(trim(city_name)) = lower('Batatais');
UPDATE professional_profiles SET city_id = 'franca'         WHERE lower(trim(city_name)) = lower('Franca');
UPDATE professional_profiles SET city_id = 'ribeirao-preto' WHERE lower(trim(city_name)) = lower('Ribeirão Preto');

CREATE UNIQUE INDEX idx_cities_name ON cities(name);

CREATE INDEX idx_professional_profiles_city_id ON professional_profiles(city_id);
