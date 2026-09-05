ALTER TABLE users
    ADD COLUMN default_shipping_address_enc TEXT NULL,
    ADD COLUMN shipping_address_version BIGINT NOT NULL DEFAULT 0;
