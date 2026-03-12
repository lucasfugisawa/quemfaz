-- V6: Profile View Events

CREATE TABLE profile_view_events (
    id TEXT PRIMARY KEY,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
    city_name TEXT,
    source TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profile_view_profile_id ON profile_view_events(professional_profile_id);
CREATE INDEX idx_profile_view_user_id ON profile_view_events(user_id);
CREATE INDEX idx_profile_view_created_at ON profile_view_events(created_at);
