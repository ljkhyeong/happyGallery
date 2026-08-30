ALTER TABLE phone_verifications
    ADD COLUMN delivered BOOLEAN NOT NULL DEFAULT FALSE AFTER code_enc;
