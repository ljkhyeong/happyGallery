ALTER TABLE bookings
    ADD COLUMN balance_paid_at DATETIME(6) NULL AFTER balance_status;

-- 기존 결제 완료 예약은 생성 시각을 결제 완료 시각의 최선 근사값으로 사용한다.
UPDATE bookings
SET deposit_paid_at = created_at
WHERE pass_purchase_id IS NULL
  AND deposit_amount > 0
  AND deposit_paid_at IS NULL;

UPDATE bookings
SET balance_status = 'PAID',
    arrears_flag = FALSE
WHERE balance_amount = 0;

UPDATE bookings
SET balance_paid_at = created_at,
    arrears_flag = FALSE
WHERE balance_status = 'PAID'
  AND balance_amount > 0
  AND balance_paid_at IS NULL;

CREATE INDEX idx_bookings_balance_paid_at
    ON bookings (balance_paid_at);

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_balance_payment
        CHECK (
            (balance_status = 'UNPAID' AND balance_paid_at IS NULL)
            OR (balance_status = 'PAID' AND (balance_amount = 0 OR balance_paid_at IS NOT NULL))
        ),
    ADD CONSTRAINT chk_bookings_arrears_unpaid
        CHECK (arrears_flag = FALSE OR balance_status = 'UNPAID');
