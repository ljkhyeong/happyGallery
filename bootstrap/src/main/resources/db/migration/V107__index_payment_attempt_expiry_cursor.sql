-- 결제 준비 만료 배치의 ID 키셋 순회를 정렬 없이 지원한다.
-- status와 id로 후보를 한 방향으로 훑고 created_at은 인덱스 안에서 만료 여부를 확인한다.
CREATE INDEX idx_payment_attempt_status_id_created
    ON payment_attempt (status, id, created_at);
