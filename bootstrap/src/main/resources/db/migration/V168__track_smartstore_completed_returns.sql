ALTER TABLE smartstore_product_orders
    ADD COLUMN completed_return_quantity INT NULL,
    ADD COLUMN reviewed_return_quantity INT NOT NULL DEFAULT 0,
    ADD COLUMN restored_return_quantity INT NOT NULL DEFAULT 0;
