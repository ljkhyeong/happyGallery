ALTER TABLE payment_attempt
    ADD COLUMN confirmed_payment_method VARCHAR(30) NULL AFTER confirmed_payment_key;
