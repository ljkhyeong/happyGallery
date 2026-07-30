-- 회원 전용 8회권 전환 뒤 남아 있을 수 있는 소유자 없는 행을 자동 보정하지 않는다.
-- 기존 NULL 행이 있으면 이 atomic ALTER 전체를 실패시켜 부분 적용을 남기지 않는다.
ALTER TABLE pass_purchases
    DROP FOREIGN KEY fk_pass_user,
    MODIFY COLUMN user_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_pass_user_v105
        FOREIGN KEY (user_id) REFERENCES users (id);
