ALTER TABLE pass_purchases
    ADD COLUMN plan_code VARCHAR(30) NULL
        COMMENT '구매 시점 8회권 정책 코드' AFTER expires_at;

-- 정책 도입 전에 판매된 이용권은 당시의 모든 클래스 사용 계약을 유지한다.
UPDATE pass_purchases
SET plan_code = 'LEGACY_ALL_CLASSES'
WHERE plan_code IS NULL;

ALTER TABLE pass_purchases
    MODIFY COLUMN plan_code VARCHAR(30) NOT NULL
        COMMENT '구매 시점 8회권 정책 코드';
