-- ==========================================================
-- V2: 핵심 테이블 초기 생성
-- 생성 순서: 의존 없는 테이블 → 참조 테이블 순
-- ==========================================================

-- ----------------------------------------------------------
-- 1. users / guests
-- ----------------------------------------------------------
CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE guests
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    phone_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

-- ----------------------------------------------------------
-- 2. products / inventory
-- ----------------------------------------------------------
CREATE TABLE products
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(20)  NOT NULL COMMENT 'ProductType: READY_STOCK | MADE_TO_ORDER',
    price      BIGINT       NOT NULL COMMENT '원(KRW) 단위',
    status     VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ProductStatus: ACTIVE | INACTIVE',
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE inventory
(
    product_id BIGINT     NOT NULL PRIMARY KEY,
    quantity   INT        NOT NULL DEFAULT 1,
    version    BIGINT     NOT NULL DEFAULT 0 COMMENT '낙관적 락',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- ----------------------------------------------------------
-- 3. classes / slots
-- ----------------------------------------------------------
CREATE TABLE classes
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(30)  NOT NULL COMMENT 'PERFUME | WOOD | KNIT | POP | ...',
    duration_min INT          NOT NULL,
    price        BIGINT       NOT NULL COMMENT '원(KRW) 단위',
    buffer_min   INT          NOT NULL DEFAULT 30 COMMENT '뒤쪽 버퍼(분)',
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE slots
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id     BIGINT      NOT NULL,
    start_at     DATETIME(6) NOT NULL,
    end_at       DATETIME(6) NOT NULL,
    capacity     INT         NOT NULL DEFAULT 8,
    booked_count INT         NOT NULL DEFAULT 0,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_slot_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT uq_slot_class_start UNIQUE (class_id, start_at)
);

-- ----------------------------------------------------------
-- 4. bookings / booking_history
-- ----------------------------------------------------------
CREATE TABLE bookings
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NULL,
    guest_id        BIGINT      NULL,
    class_id        BIGINT      NOT NULL,
    slot_id         BIGINT      NOT NULL,
    status          VARCHAR(15) NOT NULL COMMENT 'BookingStatus: BOOKED | CANCELED | NO_SHOW | COMPLETED',
    deposit_amount  BIGINT      NOT NULL COMMENT '원(KRW) 단위',
    deposit_paid_at DATETIME(6) NULL,
    balance_amount  BIGINT      NOT NULL DEFAULT 0,
    balance_status  VARCHAR(10) NOT NULL DEFAULT 'UNPAID' COMMENT 'BalanceStatus: UNPAID | PAID',
    arrears_flag    BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '미수금 플래그',
    version         BIGINT      NOT NULL DEFAULT 0 COMMENT '낙관적 락',
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_booking_guest FOREIGN KEY (guest_id) REFERENCES guests (id),
    CONSTRAINT fk_booking_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES slots (id)
);

CREATE TABLE booking_history
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT      NOT NULL,
    action      VARCHAR(15) NOT NULL COMMENT 'BookingHistoryAction: BOOKED | RESCHEDULED | CANCELED | NO_SHOW | COMPLETED',
    from_slot_id BIGINT     NULL,
    to_slot_id  BIGINT      NULL,
    actor       VARCHAR(10) NOT NULL COMMENT 'CUSTOMER | ADMIN',
    reason      VARCHAR(500) NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_bh_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

-- ----------------------------------------------------------
-- 5. pass_purchases / pass_ledger
-- ----------------------------------------------------------
CREATE TABLE pass_purchases
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT      NULL,
    guest_id          BIGINT      NULL,
    purchased_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at        DATETIME(6) NOT NULL COMMENT 'purchased_at + 90일',
    total_credits     INT         NOT NULL DEFAULT 8,
    remaining_credits INT         NOT NULL DEFAULT 8,
    CONSTRAINT fk_pass_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_pass_guest FOREIGN KEY (guest_id) REFERENCES guests (id)
);

CREATE TABLE pass_ledger
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    pass_purchase_id    BIGINT      NOT NULL,
    type                VARCHAR(10) NOT NULL COMMENT 'PassLedgerType: EARN | USE | REFUND | EXPIRE',
    amount              INT         NOT NULL,
    related_booking_id  BIGINT      NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ledger_pass FOREIGN KEY (pass_purchase_id) REFERENCES pass_purchases (id),
    CONSTRAINT fk_ledger_booking FOREIGN KEY (related_booking_id) REFERENCES bookings (id)
);

-- ----------------------------------------------------------
-- 6. orders / order_items / order_approvals / fulfillments / refunds
-- ----------------------------------------------------------
CREATE TABLE orders
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT      NULL,
    guest_id             BIGINT      NULL,
    status               VARCHAR(30) NOT NULL COMMENT 'OrderStatus enum',
    total_amount         BIGINT      NOT NULL COMMENT '원(KRW) 단위',
    paid_at              DATETIME(6) NULL,
    approval_deadline_at DATETIME(6) NULL COMMENT '결제 후 +24h, SLA 배치 기준',
    bundle_id            BIGINT      NULL COMMENT '번들 결제 그룹 ID',
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_order_guest FOREIGN KEY (guest_id) REFERENCES guests (id)
);

CREATE TABLE order_items
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    qty        INT    NOT NULL DEFAULT 1,
    unit_price BIGINT NOT NULL,
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE order_approvals
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id           BIGINT       NOT NULL,
    decided_by_admin_id BIGINT      NULL,
    decision           VARCHAR(10)  NOT NULL COMMENT 'APPROVE | REJECT | DELAY',
    reason             VARCHAR(500) NULL,
    decided_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_approval_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE fulfillments
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id           BIGINT       NOT NULL,
    type               VARCHAR(10)  NOT NULL COMMENT 'FulfillmentType: SHIPPING | PICKUP',
    status             VARCHAR(30)  NOT NULL COMMENT 'OrderStatus 중 이행 관련 상태',
    address            VARCHAR(500) NULL COMMENT '배송지 (SHIPPING)',
    pickup_store       VARCHAR(255) NULL COMMENT '픽업 매장명 (PICKUP)',
    expected_ship_date DATE         NULL,
    pickup_deadline_at DATETIME(6)  NULL,
    CONSTRAINT fk_fulfillment_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE refunds
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT       NULL COMMENT '주문 환불',
    booking_id  BIGINT       NULL COMMENT '예약금 환불',
    amount      BIGINT       NOT NULL,
    status      VARCHAR(10)  NOT NULL COMMENT 'RefundStatus: REQUESTED | SUCCEEDED | FAILED',
    pg_ref      VARCHAR(255) NULL COMMENT 'PG사 환불 참조번호',
    fail_reason VARCHAR(500) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_refund_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

-- ----------------------------------------------------------
-- 7. notification_log
-- ----------------------------------------------------------
CREATE TABLE notification_log
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NULL,
    guest_id   BIGINT       NULL,
    channel    VARCHAR(10)  NOT NULL COMMENT 'KAKAO | SMS | EMAIL | PUSH',
    event_type VARCHAR(50)  NOT NULL COMMENT '예: BOOKING_CONFIRMED, DEPOSIT_REFUNDED, REMINDER_D1',
    status     VARCHAR(10)  NOT NULL COMMENT 'SUCCESS | FAILED',
    fail_reason VARCHAR(500) NULL,
    sent_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

-- ==========================================================
-- Indexes
-- ==========================================================

-- 승인 SLA 배치 조회: status별 마감 시간 범위 스캔
CREATE INDEX idx_orders_status_deadline ON orders (status, approval_deadline_at);

-- 환불 실패 재처리 배치 조회
CREATE INDEX idx_refunds_status_created ON refunds (status, created_at);

-- 8회권 만료 배치 조회
CREATE INDEX idx_pass_purchases_expires ON pass_purchases (expires_at);

-- 예약 슬롯별 조회 (정원 확인, 변경 등)
CREATE INDEX idx_bookings_slot ON bookings (slot_id);

-- 사용자/게스트별 예약 조회
CREATE INDEX idx_bookings_user ON bookings (user_id);
CREATE INDEX idx_bookings_guest ON bookings (guest_id);

-- 알림 로그 발송 이력 조회
CREATE INDEX idx_notification_log_sent ON notification_log (sent_at);
