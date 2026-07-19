ALTER TABLE email_verification_token
    ADD COLUMN new_email VARCHAR(255);
ALTER TABLE email_verification_token
    ADD CONSTRAINT uq_evt_user UNIQUE (user_id);