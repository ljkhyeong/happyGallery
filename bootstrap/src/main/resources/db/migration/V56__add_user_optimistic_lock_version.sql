ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER credential_version;
