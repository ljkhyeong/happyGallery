ALTER TABLE refunds
    DROP CHECK chk_refunds_amount_positive;

ALTER TABLE refunds
    ADD COLUMN customer_refund_amount BIGINT NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN reward_restore_amount BIGINT NOT NULL DEFAULT 0 AFTER customer_refund_amount,
    ADD COLUMN reward_revoke_amount BIGINT NOT NULL DEFAULT 0 AFTER reward_restore_amount,
    ADD COLUMN restore_coupon BOOLEAN NOT NULL DEFAULT FALSE AFTER reward_revoke_amount;

UPDATE refunds
SET customer_refund_amount = amount;

ALTER TABLE refunds
    ADD CONSTRAINT chk_refunds_mixed_benefit_amounts CHECK (
        amount >= 0
        AND customer_refund_amount >= 0
        AND reward_restore_amount >= 0
        AND reward_revoke_amount >= 0
        AND customer_refund_amount = amount + reward_restore_amount
        AND (customer_refund_amount > 0 OR reward_revoke_amount > 0 OR restore_coupon)
        AND (
            order_id IS NOT NULL
            OR (
                amount > 0
                AND customer_refund_amount = amount
                AND reward_restore_amount = 0
                AND reward_revoke_amount = 0
                AND restore_coupon = FALSE
            )
        )
        AND (order_claim_id IS NULL OR restore_coupon = FALSE)
        AND (restore_coupon = FALSE OR (order_id IS NOT NULL AND order_claim_id IS NULL))
    );

ALTER TABLE order_claim_items
    ADD COLUMN approved_reward_restore_amount BIGINT NULL AFTER approved_refund_amount;

UPDATE order_claim_items
SET approved_reward_restore_amount = 0
WHERE approved_refund_amount IS NOT NULL;

ALTER TABLE order_claim_items
    ADD CONSTRAINT chk_order_claim_item_reward_restore_amount CHECK (
        (approved_refund_amount IS NULL AND approved_reward_restore_amount IS NULL)
        OR (
            approved_refund_amount IS NOT NULL
            AND approved_reward_restore_amount IS NOT NULL
            AND approved_reward_restore_amount BETWEEN 0 AND approved_refund_amount
        )
    );

ALTER TABLE issued_coupons
    DROP CHECK chk_issued_coupon_state;

ALTER TABLE issued_coupons
    ADD CONSTRAINT chk_issued_coupon_state CHECK (
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
        (status = 'EXPIRED'
            AND (
                (payment_attempt_id IS NULL
                    AND used_order_id IS NULL
                    AND reserved_at IS NULL
                    AND used_at IS NULL)
                OR
                (payment_attempt_id IS NOT NULL
                    AND used_order_id IS NOT NULL
                    AND reserved_at IS NOT NULL
                    AND used_at IS NOT NULL)
            ))
        OR
        (status = 'CANCELED'
            AND payment_attempt_id IS NULL
            AND used_order_id IS NULL
            AND reserved_at IS NULL
            AND used_at IS NULL)
    );
