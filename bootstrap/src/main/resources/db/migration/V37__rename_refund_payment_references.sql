UPDATE refunds
SET refund_pg_ref = NULL
WHERE refund_pg_ref = pg_ref;

ALTER TABLE refunds
    CHANGE COLUMN pg_ref payment_key VARCHAR(255) NULL COMMENT 'Toss paymentKey (원결제 식별자)';

ALTER TABLE refunds
    CHANGE COLUMN refund_pg_ref refund_transaction_key VARCHAR(255) NULL COMMENT 'Toss cancel transactionKey (환불 거래 식별자)';
