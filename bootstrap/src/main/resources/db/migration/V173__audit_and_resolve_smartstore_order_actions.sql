CREATE TABLE smartstore_order_action_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_order_id VARCHAR(30) NOT NULL,
    action VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_summary TEXT NULL,
    result_code VARCHAR(100) NULL,
    result_message VARCHAR(1000) NULL,
    changed_by_admin_id BIGINT NULL,
    changed_by VARCHAR(100) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_smartstore_order_action_history_action CHECK (action IN (
        'INVENTORY_RESOLVED',
        'ORDER_CONFIRMED',
        'ORDER_DISPATCHED',
        'ORDER_DELAYED',
        'CANCEL_APPROVED',
        'RETURN_APPROVED',
        'RETURN_REJECTED',
        'RETURN_HELD',
        'RETURN_HOLD_RELEASED',
        'RETURN_REQUESTED',
        'EXCHANGE_DISPATCHED',
        'EXCHANGE_COLLECTION_COMPLETED',
        'EXCHANGE_REJECTED',
        'EXCHANGE_HELD',
        'EXCHANGE_HOLD_RELEASED',
        'CANCEL_REQUESTED'
    )),
    CONSTRAINT ck_smartstore_order_action_history_status CHECK (status IN (
        'REQUESTED', 'SUCCEEDED', 'REJECTED', 'RESULT_UNKNOWN'
    ))
);

CREATE INDEX idx_smartstore_order_action_history_order
    ON smartstore_order_action_history (product_order_id, requested_at DESC, id DESC);
