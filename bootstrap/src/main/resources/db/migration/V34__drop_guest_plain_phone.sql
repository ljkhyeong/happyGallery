-- 게스트 전화번호 평문 컬럼 제거
-- 실행 전 기존 guests.phone 값은 phone_enc/phone_hmac로 백필되어 있어야 한다.

ALTER TABLE guests
    MODIFY COLUMN phone_enc  VARCHAR(255) NOT NULL,
    MODIFY COLUMN phone_hmac CHAR(64)     NOT NULL,
    DROP COLUMN phone;
