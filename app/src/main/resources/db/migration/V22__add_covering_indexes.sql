-- ==========================================================
-- V22: 커버링 인덱스 및 쿼리 패턴 매칭 인덱스 추가
-- ==========================================================

-- 관리자 주문 상태별 목록 (findByStatusOrderByCreatedAtDesc)
-- 기존 idx_orders_status_deadline은 (status, approval_deadline_at)이므로
-- ORDER BY created_at DESC 정렬에 불일치 → filesort 발생
CREATE INDEX idx_orders_status_created ON orders (status, created_at DESC);

-- 공개 슬롯 조회 (findAvailableByClassAndDate)
-- WHERE class_id = ? AND is_active = true AND start_at BETWEEN ? AND ?
-- 가장 빈번한 공개 API — 인덱스 없이 풀스캔
CREATE INDEX idx_slots_class_active_start ON slots (class_id, is_active, start_at);

-- 8회권 만료 배치 (findByExpiresAtBeforeAndRemainingCreditsGreaterThan)
-- 기존 idx_pass_purchases_expires는 (expires_at) 단일
-- remaining_credits 필터를 Index Condition Pushdown으로 처리
CREATE INDEX idx_pass_purchases_expires_credits ON pass_purchases (expires_at, remaining_credits);

-- 알림 중복 체크 — 게스트 (existsByGuestIdAndEventTypeAndStatusAndSentAtBetween)
-- 기존 idx_notification_log_sent는 (sent_at) 단일 → EXISTS에서 풀스캔
CREATE INDEX idx_noti_log_guest_event ON notification_log (guest_id, event_type, status, sent_at);

-- 알림 중복 체크 — 회원 (existsByUserIdAndEventTypeAndStatusAndSentAtBetween)
CREATE INDEX idx_noti_log_user_event ON notification_log (user_id, event_type, status, sent_at);
