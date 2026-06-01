CREATE TABLE email_verification_token (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);