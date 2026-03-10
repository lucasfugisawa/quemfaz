-- V2: Auth Vertical Schema

CREATE TYPE user_status AS ENUM ('ACTIVE', 'BLOCKED');

CREATE TABLE users (
    id TEXT PRIMARY KEY, -- Using text-based IDs (ULID recommended by context if present, otherwise UUID strings)
    name TEXT,
    photo_url TEXT,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_phone_auth_identities (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone_number TEXT NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_phone_number UNIQUE (phone_number)
);

CREATE INDEX idx_user_phone_auth_identities_user_id ON user_phone_auth_identities(user_id);

CREATE TABLE otp_challenges (
    id TEXT PRIMARY KEY,
    phone_number TEXT NOT NULL,
    otp_code_hash TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_challenges_phone_number ON otp_challenges(phone_number);
CREATE INDEX idx_otp_challenges_active ON otp_challenges(phone_number) WHERE consumed_at IS NULL;
