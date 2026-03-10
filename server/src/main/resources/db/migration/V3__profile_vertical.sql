-- V3: Professional Profile Vertical Schema

CREATE TYPE profile_completeness AS ENUM ('INCOMPLETE', 'COMPLETE');
CREATE TYPE profile_status AS ENUM ('DRAFT', 'PUBLISHED', 'BLOCKED');
CREATE TYPE service_match_level AS ENUM ('PRIMARY', 'SECONDARY', 'RELATED');

CREATE TABLE professional_profiles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    description TEXT,
    normalized_description TEXT,
    contact_phone TEXT,
    whatsapp_phone TEXT,
    city_name TEXT,
    completeness profile_completeness NOT NULL DEFAULT 'INCOMPLETE',
    status profile_status NOT NULL DEFAULT 'DRAFT',
    last_active_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_professional_profiles_user_id ON professional_profiles(user_id);
CREATE INDEX idx_professional_profiles_city_name ON professional_profiles(city_name);
CREATE INDEX idx_professional_profiles_status ON professional_profiles(status);

CREATE TABLE professional_profile_neighborhoods (
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    neighborhood_name TEXT NOT NULL,
    PRIMARY KEY (professional_profile_id, neighborhood_name)
);

CREATE INDEX idx_ppn_profile_id ON professional_profile_neighborhoods(professional_profile_id);

CREATE TABLE professional_profile_services (
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    service_id TEXT NOT NULL,
    match_level service_match_level NOT NULL,
    PRIMARY KEY (professional_profile_id, service_id)
);

CREATE INDEX idx_pps_profile_id ON professional_profile_services(professional_profile_id);

CREATE TABLE professional_profile_portfolio_photos (
    id TEXT PRIMARY KEY,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    caption TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pppp_profile_id ON professional_profile_portfolio_photos(professional_profile_id);
