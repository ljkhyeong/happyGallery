CREATE TABLE policy_consents
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT      NULL,
    payment_attempt_id BIGINT      NULL,
    consent_type       VARCHAR(30) NOT NULL,
    purpose            VARCHAR(40) NOT NULL,
    policy_version     VARCHAR(40) NOT NULL,
    accepted_at        DATETIME(6) NOT NULL,
    CONSTRAINT fk_policy_consent_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_policy_consent_payment_attempt
        FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt (id),
    CONSTRAINT chk_policy_consent_subject
        CHECK (
            (user_id IS NOT NULL AND payment_attempt_id IS NULL)
            OR (user_id IS NULL AND payment_attempt_id IS NOT NULL)
        )
);

CREATE INDEX idx_policy_consents_user
    ON policy_consents (user_id, accepted_at DESC, id DESC);

CREATE INDEX idx_policy_consents_payment_attempt
    ON policy_consents (payment_attempt_id, accepted_at DESC, id DESC);
