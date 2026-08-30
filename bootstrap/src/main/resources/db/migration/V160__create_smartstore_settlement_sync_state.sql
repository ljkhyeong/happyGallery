CREATE TABLE smartstore_settlement_sync_state (
    id BIGINT NOT NULL,
    next_pay_date DATE NOT NULL,
    processing_started_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_smartstore_settlement_sync_singleton CHECK (id = 1)
);

INSERT INTO smartstore_settlement_sync_state (id, next_pay_date)
VALUES (1, DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY));
