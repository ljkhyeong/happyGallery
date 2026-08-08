CREATE TABLE coupon_definitions
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    discount_type       VARCHAR(20)  NOT NULL,
    discount_value      BIGINT       NOT NULL,
    min_order_amount    BIGINT       NOT NULL,
    max_discount_amount BIGINT       NULL,
    valid_from          DATETIME(6)  NOT NULL,
    valid_until         DATETIME(6)  NOT NULL,
    active              BOOLEAN      NOT NULL,
    publicly_claimable  BOOLEAN      NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_coupon_definition_discount
        CHECK (
            (discount_type = 'FIXED'
                AND discount_value BETWEEN 1 AND 9007199254740991
                AND max_discount_amount IS NULL)
            OR
            (discount_type = 'PERCENT'
                AND discount_value BETWEEN 1 AND 100
                AND max_discount_amount BETWEEN 1 AND 9007199254740991)
        ),
    CONSTRAINT chk_coupon_definition_min_order
        CHECK (min_order_amount BETWEEN 0 AND 9007199254740991),
    CONSTRAINT chk_coupon_definition_validity
        CHECK (valid_from < valid_until),
    CONSTRAINT chk_coupon_definition_flags
        CHECK (active IN (0, 1) AND publicly_claimable IN (0, 1))
);

CREATE TABLE issued_coupons
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    definition_id      BIGINT      NOT NULL,
    user_id            BIGINT      NOT NULL,
    status             VARCHAR(20) NOT NULL,
    payment_attempt_id BIGINT      NULL,
    used_order_id      BIGINT      NULL,
    claimed_at         DATETIME(6) NOT NULL,
    reserved_at        DATETIME(6) NULL,
    used_at            DATETIME(6) NULL,
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_issued_coupons_user_definition
        UNIQUE (user_id, definition_id),
    CONSTRAINT uq_issued_coupons_payment_attempt
        UNIQUE (payment_attempt_id),
    CONSTRAINT uq_issued_coupons_used_order
        UNIQUE (used_order_id),
    CONSTRAINT fk_issued_coupon_definition
        FOREIGN KEY (definition_id) REFERENCES coupon_definitions (id),
    CONSTRAINT fk_issued_coupon_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_issued_coupon_payment_attempt
        FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt (id),
    CONSTRAINT fk_issued_coupon_used_order
        FOREIGN KEY (used_order_id) REFERENCES orders (id),
    CONSTRAINT chk_issued_coupon_state
        CHECK (
            (status = 'AVAILABLE'
                AND payment_attempt_id IS NULL
                AND used_order_id IS NULL
                AND reserved_at IS NULL
                AND used_at IS NULL)
            OR
            (status = 'RESERVED'
                AND payment_attempt_id IS NOT NULL
                AND used_order_id IS NULL
                AND reserved_at IS NOT NULL
                AND used_at IS NULL)
            OR
            (status = 'REDEEMED'
                AND payment_attempt_id IS NOT NULL
                AND used_order_id IS NOT NULL
                AND reserved_at IS NOT NULL
                AND used_at IS NOT NULL)
            OR
            (status IN ('EXPIRED', 'CANCELED')
                AND payment_attempt_id IS NULL
                AND used_order_id IS NULL
                AND reserved_at IS NULL
                AND used_at IS NULL)
        )
);

CREATE INDEX idx_issued_coupons_user_claimed
    ON issued_coupons (user_id, claimed_at DESC, id DESC);

CREATE INDEX idx_issued_coupons_definition
    ON issued_coupons (definition_id);
