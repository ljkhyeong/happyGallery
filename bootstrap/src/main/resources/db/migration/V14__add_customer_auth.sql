-- ==========================================================
-- V14: 고객 회원 인증 기반
-- users 테이블 보강 + user_sessions 테이블 생성
-- ==========================================================

-- users 테이블에 phone_verified, last_login_at 추가
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN last_login_at DATETIME(6) NULL;

-- 고객 세션 테이블
CREATE TABLE user_sessions
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    session_token_hash VARCHAR(64)  NOT NULL,
    expires_at         DATETIME(6)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_user_session_token UNIQUE (session_token_hash)
);

CREATE INDEX idx_user_sessions_expires ON user_sessions (expires_at);
CREATE INDEX idx_user_sessions_user ON user_sessions (user_id);
