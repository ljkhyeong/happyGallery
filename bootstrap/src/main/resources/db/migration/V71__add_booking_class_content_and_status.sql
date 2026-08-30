ALTER TABLE classes
    ADD COLUMN description TEXT NULL AFTER buffer_min,
    ADD COLUMN image_url VARCHAR(500) NULL AFTER description,
    ADD COLUMN preparation_info VARCHAR(2000) NULL AFTER image_url,
    ADD COLUMN target_audience VARCHAR(1000) NULL AFTER preparation_info,
    ADD COLUMN pass_eligible BOOLEAN NULL AFTER target_audience,
    ADD COLUMN status VARCHAR(10) NULL AFTER pass_eligible;

UPDATE classes
SET status = 'ACTIVE',
    pass_eligible = CASE WHEN category = 'PERFUME' THEN FALSE ELSE TRUE END
WHERE status IS NULL OR pass_eligible IS NULL;

ALTER TABLE classes
    MODIFY COLUMN pass_eligible BOOLEAN NOT NULL,
    MODIFY COLUMN status VARCHAR(10) NOT NULL;
