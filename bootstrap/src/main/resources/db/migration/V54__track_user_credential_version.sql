ALTER TABLE users
    ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0 AFTER password_hash;
