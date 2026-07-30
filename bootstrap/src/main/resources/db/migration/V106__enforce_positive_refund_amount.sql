-- 환불은 PG에 전달되는 금전 요청이므로 0원 후속 작업과 분리해 항상 양수만 저장한다.
-- 기존 0원 이하 행이 있으면 이 atomic ALTER 전체를 실패시켜 제약의 부분 적용을 남기지 않는다.
ALTER TABLE refunds
    ADD CONSTRAINT chk_refunds_amount_positive
        CHECK (amount > 0);
