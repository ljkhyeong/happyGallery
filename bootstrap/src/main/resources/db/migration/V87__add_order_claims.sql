CREATE TABLE order_claims
(
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id                    BIGINT        NOT NULL,
    claim_type                  VARCHAR(30)   NOT NULL,
    requested_resolution        VARCHAR(30)   NOT NULL,
    status                      VARCHAR(30)   NOT NULL,
    customer_reason             VARCHAR(1000) NOT NULL,
    admin_note                  VARCHAR(1000) NULL,
    resolved_by_admin_id        BIGINT        NULL,
    completed_by_admin_id       BIGINT        NULL,
    replacement_carrier         VARCHAR(100)  NULL,
    replacement_tracking_number VARCHAR(100)  NULL,
    requested_at                DATETIME(6)   NOT NULL,
    resolved_at                 DATETIME(6)   NULL,
    completed_at                DATETIME(6)   NULL,
    version                     BIGINT        NOT NULL DEFAULT 0,
    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_order_claim_id_order UNIQUE (id, order_id),
    CONSTRAINT fk_order_claim_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE UNIQUE INDEX uq_order_items_id_order
    ON order_items (id, order_id);

CREATE TABLE order_claim_items
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id               BIGINT NOT NULL,
    order_id               BIGINT NOT NULL,
    order_item_id          BIGINT NOT NULL,
    quantity               INT    NOT NULL,
    approved_refund_amount BIGINT NULL,
    CONSTRAINT fk_order_claim_item_claim_order
        FOREIGN KEY (claim_id, order_id) REFERENCES order_claims (id, order_id),
    CONSTRAINT fk_order_claim_item_item_order
        FOREIGN KEY (order_item_id, order_id) REFERENCES order_items (id, order_id),
    CONSTRAINT uq_order_claim_item UNIQUE (claim_id, order_item_id),
    CONSTRAINT chk_order_claim_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_claim_item_refund_amount
        CHECK (approved_refund_amount IS NULL OR approved_refund_amount >= 0)
);

CREATE INDEX idx_order_claims_status_requested
    ON order_claims (status, requested_at DESC, id DESC);

CREATE INDEX idx_order_claims_order
    ON order_claims (order_id, requested_at DESC, id DESC);

CREATE INDEX idx_order_claim_items_order_item
    ON order_claim_items (order_item_id);

-- 기존 전체 주문 환불은 주문당 하나를 유지하되, 사후 클레임 환불은 클레임당 하나를 허용한다.
CREATE INDEX idx_refunds_order
    ON refunds (order_id);

DROP INDEX uq_refunds_order ON refunds;

ALTER TABLE refunds
    ADD COLUMN order_claim_id BIGINT NULL AFTER order_id,
    ADD COLUMN direct_order_id BIGINT
        GENERATED ALWAYS AS (
            CASE WHEN order_claim_id IS NULL THEN order_id ELSE NULL END
        ) STORED,
    ADD CONSTRAINT fk_refund_order_claim_source
        FOREIGN KEY (order_claim_id, order_id) REFERENCES order_claims (id, order_id),
    ADD CONSTRAINT chk_refund_order_claim_source
        CHECK (order_claim_id IS NULL OR order_id IS NOT NULL);

CREATE UNIQUE INDEX uq_refunds_direct_order
    ON refunds (direct_order_id);

CREATE UNIQUE INDEX uq_refunds_order_claim
    ON refunds (order_claim_id);
