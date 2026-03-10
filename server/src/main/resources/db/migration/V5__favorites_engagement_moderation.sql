-- V5: Favorites, Contact Tracking, and Reporting
CREATE TABLE favorites (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_profile_favorite UNIQUE (user_id, professional_profile_id)
);

CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_profile_id ON favorites(professional_profile_id);

CREATE TYPE contact_channel AS ENUM ('WHATSAPP', 'PHONE_CALL');

CREATE TABLE contact_click_events (
    id TEXT PRIMARY KEY,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
    channel contact_channel NOT NULL,
    city_name TEXT,
    source TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contact_click_profile_id ON contact_click_events(professional_profile_id);
CREATE INDEX idx_contact_click_user_id ON contact_click_events(user_id);
CREATE INDEX idx_contact_click_created_at ON contact_click_events(created_at);

CREATE TYPE report_status AS ENUM ('OPEN', 'DISMISSED', 'RESOLVED');
CREATE TYPE report_reason AS ENUM ('SPAM', 'INAPPROPRIATE_CONTENT', 'WRONG_PHONE_NUMBER', 'FAKE_PROFILE', 'ABUSIVE_BEHAVIOR', 'OTHER');
CREATE TYPE report_target_type AS ENUM ('PROFESSIONAL_PROFILE');

CREATE TABLE reports (
    id TEXT PRIMARY KEY,
    reporter_user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type report_target_type NOT NULL,
    target_id TEXT NOT NULL,
    reason report_reason NOT NULL,
    description TEXT,
    status report_status NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_action TEXT
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reporter_user_id ON reports(reporter_user_id);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
CREATE INDEX idx_reports_created_at ON reports(created_at);
