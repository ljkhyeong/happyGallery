ALTER TABLE notices
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전' AFTER view_count;

ALTER TABLE workshop_profiles
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전' AFTER updated_at;
