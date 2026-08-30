CREATE TABLE user_social_accounts
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_social_accounts_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_user_social_accounts_provider_identity
        UNIQUE (provider, provider_id),
    CONSTRAINT uq_user_social_accounts_user_provider
        UNIQUE (user_id, provider),
    CONSTRAINT chk_user_social_accounts_provider
        CHECK (provider IN ('GOOGLE', 'NAVER'))
);

INSERT INTO user_social_accounts (user_id, provider, provider_id)
SELECT id, provider, provider_id
FROM users
WHERE provider <> 'LOCAL'
  AND provider_id IS NOT NULL;
