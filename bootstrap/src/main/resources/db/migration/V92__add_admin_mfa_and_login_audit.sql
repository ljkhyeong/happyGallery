ALTER TABLE admin_user
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0 AFTER credential_version,
    ADD COLUMN locked_until DATETIME NULL AFTER failed_login_attempts,
    ADD COLUMN totp_secret_enc VARCHAR(1024) NULL AFTER locked_until,
    ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER totp_secret_enc;

CREATE TABLE admin_auth_history
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NULL,
    subject_hmac  CHAR(64) NULL,
    hmac_key_id   VARCHAR(32) NULL,
    outcome       VARCHAR(30) NOT NULL,
    created_at    DATETIME NOT NULL,
    CONSTRAINT fk_admin_auth_history_user
        FOREIGN KEY (admin_user_id) REFERENCES admin_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_admin_auth_history_user_created
    ON admin_auth_history (admin_user_id, created_at);

CREATE INDEX idx_admin_auth_history_subject_created
    ON admin_auth_history (subject_hmac, created_at);

CREATE INDEX idx_admin_auth_history_created_id
    ON admin_auth_history (created_at, id);

CREATE TABLE admin_mfa_challenge
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    token_hmac    CHAR(64) NOT NULL,
    expires_at    DATETIME NOT NULL,
    consumed_at   DATETIME NULL,
    created_at    DATETIME NOT NULL,
    CONSTRAINT uk_admin_mfa_challenge_token UNIQUE (token_hmac),
    CONSTRAINT fk_admin_mfa_challenge_user
        FOREIGN KEY (admin_user_id) REFERENCES admin_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_admin_mfa_challenge_user_created
    ON admin_mfa_challenge (admin_user_id, created_at);

CREATE TABLE admin_mfa_recovery_code
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    code_hash     VARCHAR(255) NOT NULL,
    used_at       DATETIME NULL,
    created_at    DATETIME NOT NULL,
    CONSTRAINT fk_admin_mfa_recovery_code_user
        FOREIGN KEY (admin_user_id) REFERENCES admin_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_admin_mfa_recovery_code_user_used
    ON admin_mfa_recovery_code (admin_user_id, used_at);
