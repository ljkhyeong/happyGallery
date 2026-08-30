CREATE INDEX idx_pv_expires_id
    ON phone_verifications (expires_at, id);
