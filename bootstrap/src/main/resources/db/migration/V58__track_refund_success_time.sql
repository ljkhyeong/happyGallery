ALTER TABLE refunds
    ADD COLUMN succeeded_at DATETIME(6) NULL AFTER refund_transaction_key;

UPDATE refunds
SET succeeded_at = DATE_ADD(updated_at, INTERVAL 9 HOUR),
    updated_at = updated_at
WHERE status = 'SUCCEEDED'
  AND succeeded_at IS NULL;

CREATE INDEX idx_refunds_status_succeeded_at
    ON refunds (status, succeeded_at);
