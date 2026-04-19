ALTER TABLE pass_purchases
    DROP FOREIGN KEY fk_pass_guest;

ALTER TABLE pass_purchases
    DROP COLUMN guest_id;
