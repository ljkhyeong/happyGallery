ALTER TABLE smartstore_order_action_history
    ADD COLUMN reconciliation_outcome VARCHAR(20) NULL AFTER completed_at,
    ADD COLUMN reconciliation_note VARCHAR(500) NULL AFTER reconciliation_outcome,
    ADD COLUMN reconciled_by_admin_id BIGINT NULL AFTER reconciliation_note,
    ADD COLUMN reconciled_by VARCHAR(100) NULL AFTER reconciled_by_admin_id,
    ADD COLUMN reconciled_at DATETIME(6) NULL AFTER reconciled_by,
    DROP CHECK ck_smartstore_order_action_history_status,
    ADD CONSTRAINT ck_smartstore_order_action_history_status CHECK (status IN (
        'REQUESTED', 'SUCCEEDED', 'REJECTED', 'NOT_SENT', 'RESULT_UNKNOWN'
    )),
    ADD CONSTRAINT ck_smartstore_order_action_reconciliation CHECK (
        (reconciliation_outcome IS NULL
            AND reconciliation_note IS NULL
            AND reconciled_by_admin_id IS NULL
            AND reconciled_by IS NULL
            AND reconciled_at IS NULL)
        OR
        (reconciliation_outcome IN ('APPLIED', 'NOT_APPLIED')
            AND reconciliation_note IS NOT NULL
            AND reconciled_by IS NOT NULL
            AND reconciled_at IS NOT NULL)
    );

CREATE INDEX idx_smartstore_order_action_unresolved
    ON smartstore_order_action_history (reconciled_at, requested_at DESC, id DESC);
