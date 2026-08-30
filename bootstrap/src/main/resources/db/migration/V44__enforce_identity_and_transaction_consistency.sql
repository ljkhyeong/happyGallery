-- 조회 경로가 기존 복합 인덱스로 완전히 대체되는 중복 인덱스를 제거한다.
DROP INDEX idx_cart_items_user ON cart_items;
DROP INDEX idx_pass_purchases_expires ON pass_purchases;

-- 전화번호 하나가 하나의 Guest를 식별한다. 애플리케이션은 이 제약을 이용해 원자적으로 get-or-create 한다.
DROP INDEX idx_guests_phone_hmac ON guests;
CREATE UNIQUE INDEX uq_guests_phone_hmac
    ON guests (phone_hmac);

-- 취소·완료 이력은 보존하되, 활성 예약(BOOKED)은 동일 슬롯과 예약자 조합당 한 건만 허용한다.
ALTER TABLE bookings
    ADD COLUMN active_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'BOOKED' THEN user_id ELSE NULL END) STORED,
    ADD COLUMN active_guest_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'BOOKED' THEN guest_id ELSE NULL END) STORED;

CREATE UNIQUE INDEX uq_bookings_active_user_slot
    ON bookings (slot_id, active_user_id);

CREATE UNIQUE INDEX uq_bookings_active_guest_slot
    ON bookings (slot_id, active_guest_id);

-- 위 두 UNIQUE 인덱스가 slot_id 선두 조회와 FK 인덱스 역할을 함께 수행한다.
DROP INDEX idx_bookings_slot ON bookings;

-- 현재 환불 모델은 원결제 대상당 하나의 환불 요청을 만들고 같은 행과 멱등키로 재시도한다.
CREATE UNIQUE INDEX uq_refunds_order
    ON refunds (order_id);

CREATE UNIQUE INDEX uq_refunds_booking
    ON refunds (booking_id);

CREATE UNIQUE INDEX uq_refunds_pass_purchase
    ON refunds (pass_purchase_id);

CREATE UNIQUE INDEX uq_refunds_payment_attempt
    ON refunds (payment_attempt_id);

DROP INDEX idx_refunds_payment_attempt ON refunds;

-- 예약 한 건은 8회권 크레딧 사용과 복원을 타입별로 한 번씩만 기록한다.
CREATE UNIQUE INDEX uq_pass_ledger_booking_type
    ON pass_ledger (related_booking_id, type);

-- 애플리케이션 경로 밖의 직접 쓰기에서도 슬롯과 알림 수신자 불변식을 지킨다.
ALTER TABLE slots
    ADD CONSTRAINT chk_slots_booked_count_capacity
        CHECK (booked_count >= 0 AND booked_count <= capacity),
    ADD CONSTRAINT chk_slots_time_range
        CHECK (start_at < end_at);

ALTER TABLE notification_outbox
    ADD CONSTRAINT chk_notification_outbox_recipient
        CHECK (
            (recipient_type = 'GUEST' AND guest_id IS NOT NULL AND user_id IS NULL)
            OR (recipient_type = 'USER' AND user_id IS NOT NULL AND guest_id IS NULL)
        );

ALTER TABLE notification_log
    ADD CONSTRAINT chk_notification_log_exactly_one_recipient
        CHECK (
            (guest_id IS NOT NULL AND user_id IS NULL)
            OR (guest_id IS NULL AND user_id IS NOT NULL)
        );
