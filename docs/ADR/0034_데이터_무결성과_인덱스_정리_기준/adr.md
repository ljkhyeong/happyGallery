# ADR-0034: 데이터 무결성과 인덱스 정리 기준

**날짜**: 2026-07-17
**상태**: Accepted
**갱신**: 2026-07-26

---

## 컨텍스트

애플리케이션의 중복 선조회는 읽기와 쓰기 사이에 다른 트랜잭션이 끼어드는 경쟁 조건을 막지 못한다.
반대로 조회 조건이 기존 복합 인덱스의 왼쪽 선두 컬럼으로 완전히 대체되는데도 단일 인덱스를 함께 두면
쓰기와 저장 공간 비용만 중복된다.

또한 `AUTO_INCREMENT` PK는 삽입 순번이고 `created_at`, `sent_at`, `purchased_at`은 업무 시각이므로
두 값을 같은 의미로 취급해 시간 인덱스를 일괄 제거할 수 없다.

---

## 결정 사항

### 중복 인덱스

- `cart_items(user_id)`는 `UNIQUE(user_id, product_id)`가 회원별 조회, 삭제, FK 인덱스 역할을 모두 대체하므로 제거한다.
- `pass_purchases(expires_at)`는 `(expires_at, remaining_credits)`가 현재 모든 만료 조회를 대체하므로 제거한다.
- 결제 시도 복구용 `(status, created_at)`과 기간·마감·발송·구매 시각 인덱스는 현재 조회 의미가 있으므로 유지한다.
- `phone_verifications(expires_at, id)`는 만료 후 보존 기간이 지난 인증 행을 삭제할 때 전체 스캔과 실시간 인증 행 잠금 범위를 줄이므로 유지한다.
- `cart_merge_requests(created_at, user_id, idempotency_key)`는 7일 멱등 보장 기간이 지난 기록을 작은 묶음으로 삭제할 때 사용한다. 기본 키는 사용자·멱등키 조회만 지원하므로 시간 선두 인덱스를 별도로 유지한다.
- 최신순이 실제로 삽입 순번을 뜻하는 작은 목록은 쿼리 계약을 `id DESC`로 바꾼 뒤 별도 시간 인덱스를 제거할 수 있다.
  현재 시간 커서와 기간 조회는 `ORDER BY time DESC, id DESC`를 유지하며 ID를 동률 해소 키로 사용한다.
- 주문 클레임 작업함은 상태 없는 전체 조회와 상태별 조회를 모두 제공하므로
  `(requested_at DESC, id DESC)`와 `(status, requested_at DESC, id DESC)`를 각각 유지한다.
- `notification_outbox(event_type, aggregate_type, aggregate_id)`는 예약·8회권·픽업 리마인드가
  멱등키 문자열 형식과 무관하게 이미 접수된 aggregate를 제외할 때 사용한다. 반복 가능한 다른 알림 이벤트도
  같은 의미 컬럼 조합을 가질 수 있으므로 UNIQUE로 만들지 않고, 쓰기 멱등성은 기존 `idempotency_key` UNIQUE가 담당한다.

### Guest 식별

- `guests.phone_hmac`를 UNIQUE로 두어 전화번호 하나당 Guest 한 건을 보장한다.
- 애플리케이션은 선조회 후 INSERT하지 않고 DB UNIQUE를 이용한 원자적 get-or-create를 수행한다.
- 기존 Guest가 존재하면 이름과 암호문을 덮어쓰지 않고 현재 이력 소유자를 그대로 재사용한다.

### 활성 예약 중복

- 취소·완료·결석 예약은 이력으로 보존하고, `BOOKED` 상태만 동일 슬롯과 동일 예약자 조합당 한 건으로 제한한다.
- MySQL generated column `active_user_id`, `active_guest_id`를 두고
  `(slot_id, active_user_id)`, `(slot_id, active_guest_id)`를 각각 UNIQUE로 만든다.
- 취소 후 동일 슬롯 재예약은 허용한다.
- 예약 UNIQUE 위반만 `409 DUPLICATE_BOOKING`으로 변환하고 다른 DB 제약 위반은 일반 입력 오류로 처리한다.

### 금전·원장

- 현재 환불은 예약·직접 주문·주문 클레임·8회권·결제 시도 보상 원본당 환불 요청 한 건을 만들고 같은 행과 멱등키로 재시도한다.
  직접 주문은 generated `direct_order_id`, 주문 클레임은 `order_claim_id`, 나머지는 각 source FK를 UNIQUE로 둔다.
  하나의 클레임을 다시 분할 환불하는 모델을 도입할 때는 이 결정을 다시 검토한다.
- 환불 요청 멱등키와 PG가 반환한 취소 거래 식별자는 각각 환불 전체에서 한 건에만 귀속되어야 하므로
  `idempotency_key`, `refund_transaction_key`를 UNIQUE로 둔다. nullable 거래 식별자는 성공 전까지 여러 `NULL`을 허용한다.
- 예약 한 건의 8회권 `USE`와 예약 취소 `REFUND`는 타입별 한 번만 허용하므로
  `UNIQUE(related_booking_id, type)`으로 원장 중복을 막는다.

### 기타 DB 불변식

- 슬롯은 `0 <= booked_count <= capacity`와 `start_at < end_at`을 CHECK로 강제한다.
- 알림 outbox는 `recipient_type`과 일치하는 `user_id` 또는 `guest_id` 하나만 가진다.
- 알림 로그도 `user_id`, `guest_id` 중 정확히 하나만 가진다.

---

## 마이그레이션 원칙

- 기존 중복 환불, 원장, Guest 또는 활성 예약을 임의 삭제하거나 상태 변경하지 않는다.
- UNIQUE/CHECK 추가가 기존 불일치를 발견하면 Flyway를 실패시켜 운영자가 원본 결제와 이력을 확인한 뒤 정리하도록 한다.
- UNIQUE가 같은 선두 컬럼의 일반 인덱스를 대체하면 중복 일반 인덱스를 제거한다.

---

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | 동시 요청과 새 쓰기 경로에서도 핵심 불변식을 DB가 최종 보장한다 |
| 장점 | 의미가 완전히 겹치는 보조 인덱스의 쓰기·저장 비용을 없앤다 |
| 장점 | 취소 이력을 유지하면서 정상적인 동일 슬롯 재예약을 허용한다 |
| 단점 | 기존 데이터에 불일치가 있으면 배포 마이그레이션이 중단된다 |
| 대응 | 데이터를 자동 삭제하지 않고 source와 결제 이력을 대조해 수동 정리한 뒤 재실행한다 |

---

## 구현 반영

- `V44__enforce_identity_and_transaction_consistency.sql`
- `V94__index_notification_outbox_event_aggregate.sql`
- `VerifiedGuestResolver`와 Guest persistence 원자 get-or-create 경계
- 활성 예약 기준 Booking repository 조회
- 예약 UNIQUE 제약 이름 기준 예외 변환
- 관리자 예약·주문 검색의 암호화된 Guest 전화번호 복호화
