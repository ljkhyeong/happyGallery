ALTER TABLE pass_purchases
    DROP INDEX idx_pass_purchases_user_purchased,
    ADD INDEX idx_pass_purchases_user_purchased_id
        (user_id, purchased_at DESC, id DESC);
