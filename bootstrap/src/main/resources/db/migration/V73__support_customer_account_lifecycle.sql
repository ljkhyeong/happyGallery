ALTER TABLE users
    ADD COLUMN withdrawn_at DATETIME(6) NULL AFTER last_login_at,
    ADD CONSTRAINT uq_users_phone_hmac UNIQUE (phone_hmac),
    DROP INDEX idx_users_phone_hmac;
