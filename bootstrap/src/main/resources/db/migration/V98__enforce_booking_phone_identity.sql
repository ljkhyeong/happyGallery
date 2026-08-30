-- 회원과 비회원 테이블을 가로질러 같은 현재 전화번호의 활성 예약을 하나로 식별한다.
-- 영구 DDL 전에 기존 데이터의 NULL/교차 중복을 임시 테이블 제약으로 검증한다.
DROP TEMPORARY TABLE IF EXISTS booking_phone_identity_preflight;

CREATE TEMPORARY TABLE booking_phone_identity_preflight (
    slot_id BIGINT NOT NULL,
    owner_phone_hmac CHAR(64) NOT NULL,
    UNIQUE KEY uq_booking_phone_identity_preflight (slot_id, owner_phone_hmac)
);

INSERT INTO booking_phone_identity_preflight (slot_id, owner_phone_hmac)
SELECT booking.slot_id,
       CASE
           WHEN booking.user_id IS NOT NULL THEN member.phone_hmac
           ELSE guest.phone_hmac
       END
FROM bookings booking
LEFT JOIN users member ON member.id = booking.user_id
LEFT JOIN guests guest ON guest.id = booking.guest_id
WHERE booking.status = 'BOOKED';

DROP TEMPORARY TABLE booking_phone_identity_preflight;

ALTER TABLE bookings
    ADD COLUMN owner_phone_hmac CHAR(64) NULL AFTER guest_id;

UPDATE bookings booking
LEFT JOIN users member ON member.id = booking.user_id
LEFT JOIN guests guest ON guest.id = booking.guest_id
SET booking.owner_phone_hmac =
        CASE
            WHEN booking.user_id IS NOT NULL THEN member.phone_hmac
            ELSE guest.phone_hmac
        END
WHERE booking.status = 'BOOKED';

ALTER TABLE bookings
    ADD COLUMN active_owner_phone_hmac CHAR(64)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'BOOKED' THEN owner_phone_hmac ELSE NULL END
        ) STORED,
    ADD CONSTRAINT chk_bookings_owner_phone_hmac_lifecycle
        CHECK (
            (status = 'BOOKED' AND owner_phone_hmac IS NOT NULL)
            OR (status <> 'BOOKED' AND owner_phone_hmac IS NULL)
        ),
    ADD UNIQUE INDEX uq_bookings_active_phone_slot (slot_id, active_owner_phone_hmac);
