ALTER TABLE orders
    ADD CONSTRAINT chk_orders_exactly_one_owner
        CHECK (
            (user_id IS NOT NULL AND guest_id IS NULL)
            OR (user_id IS NULL AND guest_id IS NOT NULL)
        );
