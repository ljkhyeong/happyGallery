-- ==========================================================
-- V32: payment_attempt 테이블 신설
-- - Toss Payments prepare/confirm 2단계 결제 흐름에서
--   서버가 orderId(UUID)와 amount를 소유하도록 prepare 시점에 저장한다.
-- - confirm 성공 시 paymentKey/pgRef가 채워지고 status=CONFIRMED로 전이.
-- - 미완료(PENDING) 레코드 정리 배치용 (status, created_at) 인덱스.
-- ==========================================================

CREATE TABLE payment_attempt
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id_external VARCHAR(64)  NOT NULL COMMENT 'Toss에 넘기는 외부 주문 식별자 (UUID)',
    context           VARCHAR(20)  NOT NULL COMMENT 'PaymentContext: ORDER | BOOKING | PASS',
    amount            BIGINT       NOT NULL COMMENT '원(KRW) 단위, prepare 시 확정',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PaymentAttemptStatus: PENDING | CONFIRMED | FAILED | CANCELED',
    payment_key       VARCHAR(200) NULL     COMMENT 'Toss paymentKey (confirm 성공 시)',
    pg_ref            VARCHAR(200) NULL     COMMENT 'PG 참조값 (환불 조회 시 활용)',
    payload_json      TEXT         NOT NULL COMMENT 'prepare 시점 context별 payload (serialized)',
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at      DATETIME(6)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0 COMMENT '낙관적 락',
    CONSTRAINT uq_payment_attempt_order_id_external UNIQUE (order_id_external)
);

-- 미완료 정리 배치: PENDING 상태에서 일정 시간 경과한 레코드 조회용
CREATE INDEX idx_payment_attempt_status_created
    ON payment_attempt (status, created_at);
