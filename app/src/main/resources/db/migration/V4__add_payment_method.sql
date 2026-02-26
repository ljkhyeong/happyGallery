-- ==========================================================
-- V4: 예약금 결제 수단 컬럼 추가 (§6.2)
-- - bookings.payment_method: DepositPaymentMethod (CARD | EASY_PAY | BANK_TRANSFER)
-- ==========================================================

ALTER TABLE bookings
    ADD COLUMN payment_method VARCHAR(15) NULL COMMENT 'DepositPaymentMethod: CARD | EASY_PAY | BANK_TRANSFER';
