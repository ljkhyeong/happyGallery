ALTER TABLE products
    ADD COLUMN description TEXT NULL AFTER price,
    ADD COLUMN image_url VARCHAR(500) NULL AFTER description;
