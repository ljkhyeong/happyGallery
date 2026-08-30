ALTER TABLE admin_user
    ADD COLUMN last_accepted_totp_step BIGINT NULL AFTER mfa_enabled;
