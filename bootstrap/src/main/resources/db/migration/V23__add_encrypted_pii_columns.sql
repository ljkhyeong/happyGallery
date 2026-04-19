-- 개인정보 암호화 컬럼 추가 (블라인드 인덱스 방식)
-- 기존 평문 컬럼은 백필 완료 후 별도 마이그레이션에서 제거

-- guests 테이블
ALTER TABLE guests
    ADD COLUMN phone_enc  VARCHAR(255) NULL AFTER phone,
    ADD COLUMN phone_hmac CHAR(64)     NULL AFTER phone_enc;

CREATE INDEX idx_guests_phone_hmac ON guests (phone_hmac);

-- users 테이블
ALTER TABLE users
    ADD COLUMN phone_enc  VARCHAR(255) NULL AFTER phone,
    ADD COLUMN phone_hmac CHAR(64)     NULL AFTER phone_enc,
    ADD COLUMN email_enc  VARCHAR(255) NULL AFTER email,
    ADD COLUMN email_hmac CHAR(64)     NULL AFTER email_enc;

CREATE INDEX idx_users_phone_hmac ON users (phone_hmac);
CREATE INDEX idx_users_email_hmac ON users (email_hmac);
