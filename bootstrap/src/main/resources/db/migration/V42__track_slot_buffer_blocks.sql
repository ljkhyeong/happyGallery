ALTER TABLE slots
    CHANGE COLUMN is_active admin_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN buffer_block_count INT NOT NULL DEFAULT 0 AFTER admin_active,
    ADD CONSTRAINT chk_slots_buffer_block_count_non_negative
        CHECK (buffer_block_count >= 0);

CREATE TEMPORARY TABLE slot_buffer_block_backfill
(
    slot_id     BIGINT PRIMARY KEY,
    block_count INT NOT NULL
);

INSERT INTO slot_buffer_block_backfill (slot_id, block_count)
SELECT target.id, COUNT(source.id)
FROM slots target
JOIN slots source
  ON source.class_id = target.class_id
 AND source.booked_count > 0
JOIN classes booking_class
  ON booking_class.id = source.class_id
WHERE target.start_at >= source.end_at
  AND target.start_at < TIMESTAMPADD(MINUTE, booking_class.buffer_min, source.end_at)
GROUP BY target.id;

-- 기존 is_active=false에는 비활성 원인이 없으므로 현재 예약 버퍼에 포함되는 슬롯은
-- 버퍼 차단으로 이관하고, 포함되지 않는 슬롯만 관리자 비활성 상태로 보존한다.
UPDATE slots target
JOIN slot_buffer_block_backfill backfill
  ON backfill.slot_id = target.id
SET target.buffer_block_count = backfill.block_count,
    target.admin_active = TRUE;

DROP TEMPORARY TABLE slot_buffer_block_backfill;

DROP INDEX idx_slots_class_active_start ON slots;
CREATE INDEX idx_slots_class_availability_start
    ON slots (class_id, admin_active, buffer_block_count, start_at);
