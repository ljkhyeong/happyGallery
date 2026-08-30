-- 애플리케이션이 직접 기록하는 업무 시각은 서울 현지시각이다.
-- DB UTC 기본값으로 생성됐던 금전 이벤트만 한 번 변환해 같은 조회 경계를 사용한다.
UPDATE pass_purchases
SET purchased_at = DATE_ADD(purchased_at, INTERVAL 9 HOUR);

UPDATE bookings
SET deposit_paid_at = DATE_ADD(deposit_paid_at, INTERVAL 9 HOUR)
WHERE deposit_paid_at IS NOT NULL
  AND deposit_paid_at = created_at;

UPDATE bookings
SET balance_paid_at = DATE_ADD(balance_paid_at, INTERVAL 9 HOUR)
WHERE balance_paid_at IS NOT NULL
  AND balance_paid_at = created_at;
