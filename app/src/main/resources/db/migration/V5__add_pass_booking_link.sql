-- V5: 8회권 예약 연결 + 구매 금액 추가

-- bookings: pass_purchase_id (8회권으로 결제된 예약 표시)
ALTER TABLE bookings
    ADD COLUMN pass_purchase_id BIGINT NULL COMMENT '8회권 결제 시 pass_purchases FK';

ALTER TABLE bookings
    ADD CONSTRAINT fk_booking_pass FOREIGN KEY (pass_purchase_id) REFERENCES pass_purchases (id);

-- pass_purchases: total_price (남은 횟수 정산 환불 금액 계산용)
ALTER TABLE pass_purchases
    ADD COLUMN total_price BIGINT NOT NULL DEFAULT 0 COMMENT '8회권 총 결제금액 (KRW)';
