-- ==========================================================
-- V33: orders / bookings / pass_purchases에 payment_key 컬럼 추가
-- - Toss confirm 시 확정된 paymentKey를 해당 도메인 레코드에 함께 기록.
-- - 환불 경로(RefundExecutionService)가 paymentKey 기반으로 PG 호출할 수 있도록 근거 연결.
-- - 기존 레코드 호환을 위해 NULL 허용.
-- ==========================================================

ALTER TABLE orders         ADD COLUMN payment_key VARCHAR(200) NULL COMMENT 'Toss paymentKey (결제 확정 시)';
ALTER TABLE bookings       ADD COLUMN payment_key VARCHAR(200) NULL COMMENT 'Toss paymentKey (예약금 결제 확정 시)';
ALTER TABLE pass_purchases ADD COLUMN payment_key VARCHAR(200) NULL COMMENT 'Toss paymentKey (8회권 결제 확정 시)';
