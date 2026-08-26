ALTER TABLE email_verification_token
    ADD COLUMN token_type VARCHAR(32);

UPDATE email_verification_token
SET token_type = 'EMAIL_VERIFICATION'
WHERE new_email IS NULL;

ALTER TABLE email_verification_token
    ALTER COLUMN token_type SET NOT NULL;
ALTER TABLE email_verification_token
    ALTER COLUMN token_type SET DEFAULT 'EMAIL_VERIFICATION';

ALTER TABLE email_verification_token
    ADD CONSTRAINT chk_token_type CHECK (token_type IN ('EMAIL_VERIFICATION', 'EMAIL_CHANGE', 'PASSWORD_RESET'));

ALTER TABLE email_verification_token
    DROP CONSTRAINT uq_evt_user;
ALTER TABLE email_verification_token
    ADD CONSTRAINT uq_evt_user_type UNIQUE (user_id, token_type);