ALTER TABLE admin_user
    ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0 AFTER password_hash;

CREATE TABLE admin_setup_lock
(
    id TINYINT NOT NULL PRIMARY KEY,
    CONSTRAINT chk_admin_setup_lock_singleton CHECK (id = 1)
);

INSERT INTO admin_setup_lock (id) VALUES (1);
