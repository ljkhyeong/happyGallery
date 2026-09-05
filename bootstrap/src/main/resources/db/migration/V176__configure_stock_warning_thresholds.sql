ALTER TABLE inventory
    ADD COLUMN minimum_stock INT NULL,
    ADD CONSTRAINT ck_inventory_minimum_stock CHECK (minimum_stock IS NULL OR minimum_stock >= 0);

ALTER TABLE product_variants
    ADD COLUMN minimum_stock INT NULL,
    ADD CONSTRAINT ck_variant_minimum_stock CHECK (minimum_stock IS NULL OR minimum_stock >= 0);
