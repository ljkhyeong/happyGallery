CREATE TABLE booking_calendar_settings
(
    id                    BIGINT PRIMARY KEY,
    open_time             TIME       NOT NULL,
    close_time            TIME       NOT NULL,
    slot_interval_min     INT        NOT NULL,
    block_public_holidays BOOLEAN    NOT NULL DEFAULT TRUE,
    version               BIGINT     NOT NULL DEFAULT 0,
    updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_booking_calendar_singleton CHECK (id = 1),
    CONSTRAINT chk_booking_calendar_hours CHECK (open_time < close_time),
    CONSTRAINT chk_booking_calendar_interval CHECK (slot_interval_min BETWEEN 10 AND 120)
);

INSERT INTO booking_calendar_settings
    (id, open_time, close_time, slot_interval_min, block_public_holidays)
VALUES
    (1, '10:00:00', '19:00:00', 30, TRUE);

CREATE TABLE booking_day_overrides
(
    calendar_date DATE        PRIMARY KEY,
    availability  VARCHAR(10) NOT NULL,
    reason        VARCHAR(200) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_booking_day_override_availability
        CHECK (availability IN ('OPEN', 'CLOSED'))
);

CREATE TABLE booking_time_blocks
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    calendar_date DATE         NOT NULL,
    start_time    TIME         NOT NULL,
    end_time      TIME         NOT NULL,
    reason        VARCHAR(200) NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_booking_time_block UNIQUE (calendar_date, start_time, end_time),
    CONSTRAINT chk_booking_time_block_range CHECK (start_time < end_time)
);

ALTER TABLE slots
    ADD COLUMN calendar_active BOOLEAN NOT NULL DEFAULT TRUE AFTER admin_active;

DROP INDEX idx_slots_class_availability_start ON slots;
CREATE INDEX idx_slots_class_availability_start
    ON slots (class_id, admin_active, calendar_active, buffer_block_count, start_at);

CREATE TEMPORARY TABLE slot_conflict_block_backfill
(
    slot_id     BIGINT PRIMARY KEY,
    block_count INT NOT NULL
);

INSERT INTO slot_conflict_block_backfill (slot_id, block_count)
SELECT target.id, COUNT(source.id)
FROM slots target
JOIN slots source
  ON source.class_id = target.class_id
 AND source.id <> target.id
 AND source.booked_count > 0
JOIN classes booking_class
  ON booking_class.id = source.class_id
WHERE target.start_at < TIMESTAMPADD(MINUTE, booking_class.buffer_min, source.end_at)
  AND source.start_at < TIMESTAMPADD(MINUTE, booking_class.buffer_min, target.end_at)
GROUP BY target.id;

UPDATE slots SET buffer_block_count = 0;

UPDATE slots target
JOIN slot_conflict_block_backfill backfill
  ON backfill.slot_id = target.id
SET target.buffer_block_count = backfill.block_count;

DROP TEMPORARY TABLE slot_conflict_block_backfill;
