CREATE TABLE smartstore_settlement_entries (
    entry_key VARCHAR(255) NOT NULL,
    product_order_id VARCHAR(30) NULL,
    order_id VARCHAR(30) NULL,
    product_order_type VARCHAR(50) NOT NULL,
    settle_type VARCHAR(50) NULL,
    product_name VARCHAR(4000) NULL,
    pay_settle_amount BIGINT NOT NULL,
    total_pay_commission_amount BIGINT NULL,
    selling_interlock_commission_amount BIGINT NULL,
    benefit_settle_amount BIGINT NOT NULL,
    settle_expect_amount BIGINT NOT NULL,
    settle_basis_date DATE NULL,
    settle_expect_date DATE NULL,
    settle_complete_date DATE NULL,
    pay_date DATE NULL,
    reconciliation_status VARCHAR(30) NOT NULL,
    reconciliation_reason VARCHAR(500) NULL,
    fetched_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (entry_key),
    CONSTRAINT ck_smartstore_settlement_status CHECK (reconciliation_status IN (
        'MATCHED', 'ORDER_NOT_FOUND', 'EXPECTED_AMOUNT_MISSING', 'AMOUNT_MISMATCH', 'NOT_APPLICABLE'
    ))
);

CREATE INDEX idx_smartstore_settlement_issue
    ON smartstore_settlement_entries (reconciliation_status, fetched_at DESC);
