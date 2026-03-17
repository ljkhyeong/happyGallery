-- ==========================================================
-- V17: Guest access token SHA-256 해시 저장 전환
-- - orders.access_token 컬럼 확장 (VARCHAR 36 → 64)
-- - orders.access_token UNIQUE 제약 추가 (bookings는 V3에서 이미 적용)
-- - 기존 평문 토큰은 무효화됨 (신규 토큰만 해시로 저장)
-- ==========================================================

ALTER TABLE orders MODIFY COLUMN access_token VARCHAR(64);

CREATE UNIQUE INDEX uq_orders_access_token ON orders(access_token);
