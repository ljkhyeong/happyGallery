CREATE TABLE reward_accounts
(
    user_id           BIGINT PRIMARY KEY,
    available_balance BIGINT NOT NULL DEFAULT 0,
    reserved_balance  BIGINT NOT NULL DEFAULT 0,
    debt_balance      BIGINT NOT NULL DEFAULT 0,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reward_account_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_reward_account_available CHECK (available_balance >= 0),
    CONSTRAINT chk_reward_account_reserved CHECK (reserved_balance >= 0),
    CONSTRAINT chk_reward_account_debt CHECK (debt_balance >= 0)
);

CREATE TABLE reward_lots
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    source_order_id  BIGINT      NULL,
    earned_amount    BIGINT      NOT NULL,
    remaining_amount BIGINT      NOT NULL,
    expires_at       DATETIME(6) NOT NULL,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reward_lot_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reward_lot_order FOREIGN KEY (source_order_id) REFERENCES orders (id),
    CONSTRAINT chk_reward_lot_earned_positive CHECK (earned_amount > 0),
    CONSTRAINT chk_reward_lot_remaining CHECK (
        remaining_amount >= 0 AND remaining_amount <= earned_amount
    )
);

CREATE INDEX idx_reward_lot_spendable
    ON reward_lots (user_id, expires_at, id);
CREATE INDEX idx_reward_lot_source_order
    ON reward_lots (source_order_id, id);

CREATE TABLE reward_reservations
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_attempt_id BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,
    order_id           BIGINT       NULL,
    amount             BIGINT       NOT NULL,
    restored_amount    BIGINT       NOT NULL DEFAULT 0,
    status             VARCHAR(20)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at        DATETIME(6)  NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_reward_reservation_attempt UNIQUE (payment_attempt_id),
    CONSTRAINT uq_reward_reservation_order UNIQUE (order_id),
    CONSTRAINT fk_reward_reservation_attempt
        FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt (id),
    CONSTRAINT fk_reward_reservation_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reward_reservation_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_reward_reservation_amount CHECK (amount > 0),
    CONSTRAINT chk_reward_reservation_restored CHECK (
        restored_amount >= 0 AND restored_amount <= amount
    )
);

CREATE INDEX idx_reward_reservation_user_status
    ON reward_reservations (user_id, status, id);

CREATE TABLE reward_reservation_allocations
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id  BIGINT      NOT NULL,
    reward_lot_id   BIGINT      NOT NULL,
    amount          BIGINT      NOT NULL,
    restored_amount BIGINT      NOT NULL DEFAULT 0,
    original_expiry DATETIME(6) NOT NULL,
    CONSTRAINT uq_reward_reservation_lot UNIQUE (reservation_id, reward_lot_id),
    CONSTRAINT fk_reward_allocation_reservation
        FOREIGN KEY (reservation_id) REFERENCES reward_reservations (id),
    CONSTRAINT fk_reward_allocation_lot FOREIGN KEY (reward_lot_id) REFERENCES reward_lots (id),
    CONSTRAINT chk_reward_allocation_amount CHECK (amount > 0),
    CONSTRAINT chk_reward_allocation_restored CHECK (
        restored_amount >= 0 AND restored_amount <= amount
    )
);

CREATE TABLE reward_ledger
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    type               VARCHAR(20)  NOT NULL,
    amount             BIGINT       NOT NULL,
    available_after    BIGINT       NOT NULL,
    reserved_after     BIGINT       NOT NULL,
    debt_after         BIGINT       NOT NULL,
    payment_attempt_id BIGINT       NULL,
    order_id           BIGINT       NULL,
    idempotency_key    VARCHAR(160) NOT NULL,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_reward_ledger_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_reward_ledger_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reward_ledger_attempt
        FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt (id),
    CONSTRAINT fk_reward_ledger_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_reward_ledger_amount CHECK (amount > 0),
    CONSTRAINT chk_reward_ledger_available CHECK (available_after >= 0),
    CONSTRAINT chk_reward_ledger_reserved CHECK (reserved_after >= 0),
    CONSTRAINT chk_reward_ledger_debt CHECK (debt_after >= 0)
);

CREATE INDEX idx_reward_ledger_user_created
    ON reward_ledger (user_id, created_at DESC, id DESC);
