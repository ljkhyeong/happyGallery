CREATE TABLE email_verifications
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    credential_version BIGINT       NOT NULL,
    email_hmac         CHAR(64)     NOT NULL,
    code_hmac          CHAR(64)     NOT NULL,
    code_enc           VARCHAR(255) NOT NULL,
    delivered          BOOLEAN      NOT NULL DEFAULT FALSE,
    verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at         DATETIME(6)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_email_verifications_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_email_verifications_user_email_id (user_id, email_hmac, id),
    INDEX idx_email_verifications_expiry (expires_at, id)
);
