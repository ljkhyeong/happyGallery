-- ==========================================================
-- V18: 기존 평문 access token 일괄 SHA-256 해싱
--
-- V17에서 신규 토큰만 해시로 저장하도록 전환했으나,
-- 이전에 생성된 평문 토큰(32자 UUID hex)이 DB에 남아 있으면
-- 해시 비교 기반 조회가 실패한다.
--
-- 조건: CHAR_LENGTH < 64 → 아직 해싱되지 않은 평문 토큰
-- 결과: 모든 access_token이 64자 SHA-256 hex로 통일
-- ==========================================================

UPDATE bookings
SET access_token = SHA2(access_token, 256)
WHERE access_token IS NOT NULL
  AND CHAR_LENGTH(access_token) < 64;

UPDATE orders
SET access_token = SHA2(access_token, 256)
WHERE access_token IS NOT NULL
  AND CHAR_LENGTH(access_token) < 64;
