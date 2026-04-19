-- ==========================================================
-- V10: 소셜 로그인 지원 — provider/provider_id 컬럼 추가
-- ==========================================================

-- 소셜 로그인 사용자는 비밀번호가 없으므로 nullable로 변경
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;

-- 인증 제공자 (LOCAL, GOOGLE)
ALTER TABLE users ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' AFTER password_hash;

-- 외부 제공자의 고유 식별자 (예: Google sub)
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255) NULL AFTER provider;

-- 같은 provider 내에서 provider_id 중복 방지
CREATE UNIQUE INDEX uq_users_provider_provider_id ON users (provider, provider_id);
