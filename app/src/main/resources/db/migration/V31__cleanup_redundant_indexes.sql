-- ==========================================================
-- V31: 중복/저효율 인덱스 정리
-- - notification_log 목록 조회를 user/guest 기준 sent_at 정렬에 맞춘다.
-- - orders 상태 목록은 (status, created_at, id) 커서 인덱스로 통합한다.
-- ==========================================================

-- 알림 목록 조회: /api/v1/me/notifications
-- 기존 idx_notification_log_sent(sent_at)는 user_id/guest_id 조건과 맞지 않아
-- 실제 목록 조회에서 효율이 떨어진다.
DROP INDEX idx_notification_log_sent ON notification_log;

CREATE INDEX idx_notification_log_user_sent ON notification_log (user_id, sent_at DESC);
CREATE INDEX idx_notification_log_guest_sent ON notification_log (guest_id, sent_at DESC);

-- 관리자 상태별 주문 목록은 커서 인덱스가 (status, created_at, id)를 이미 제공한다.
DROP INDEX idx_orders_status_created ON orders;
