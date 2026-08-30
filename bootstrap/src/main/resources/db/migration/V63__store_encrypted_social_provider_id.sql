ALTER TABLE user_social_accounts
    ADD COLUMN provider_id_enc VARCHAR(1024) NULL
        COMMENT '소셜 provider ID AES-GCM 암호문; 기존 행은 다음 로그인에서 채움' AFTER provider;
