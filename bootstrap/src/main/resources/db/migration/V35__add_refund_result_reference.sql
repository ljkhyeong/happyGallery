ALTER TABLE refunds
    MODIFY COLUMN pg_ref VARCHAR(255) NULL COMMENT 'PG 원결제 참조값';

ALTER TABLE refunds
    ADD COLUMN refund_pg_ref VARCHAR(255) NULL COMMENT 'PG사 환불 결과 참조번호' AFTER pg_ref;

UPDATE refunds
SET refund_pg_ref = pg_ref
WHERE status = 'SUCCEEDED'
  AND refund_pg_ref IS NULL;
