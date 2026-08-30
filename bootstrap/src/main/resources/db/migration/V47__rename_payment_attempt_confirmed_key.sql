ALTER TABLE payment_attempt
    MODIFY COLUMN payment_key VARCHAR(200) NULL COMMENT 'Toss confirm 요청 paymentKey',
    CHANGE COLUMN pg_ref confirmed_payment_key VARCHAR(200) NULL COMMENT 'Toss confirm 승인 응답 paymentKey';
