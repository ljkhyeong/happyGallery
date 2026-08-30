ALTER TABLE user_social_accounts
    DROP CHECK chk_user_social_accounts_provider,
    ADD CONSTRAINT chk_user_social_accounts_provider
        CHECK (provider IN ('GOOGLE', 'NAVER', 'KAKAO'));
