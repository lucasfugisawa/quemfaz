-- Track user acceptance of Terms of Use and Privacy Policy
ALTER TABLE users
    ADD COLUMN terms_accepted_at    TIMESTAMP,
    ADD COLUMN terms_version        VARCHAR(32),
    ADD COLUMN privacy_accepted_at  TIMESTAMP,
    ADD COLUMN privacy_version      VARCHAR(32);
