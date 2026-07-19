ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);