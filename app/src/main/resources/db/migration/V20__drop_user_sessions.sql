-- user_sessions 테이블 제거
-- 회원 세션이 Spring Session + Redis로 전환되어 DB 저장이 불필요해짐
DROP TABLE IF EXISTS user_sessions;
