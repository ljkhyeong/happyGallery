ALTER TABLE payment_attempt
    ADD COLUMN confirmed_receipt_url VARCHAR(500) NULL AFTER confirmed_payment_method;
