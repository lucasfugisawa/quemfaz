ALTER TABLE professional_profiles
    ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN contact_click_count INTEGER NOT NULL DEFAULT 0;
