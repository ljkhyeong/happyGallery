# ADR-0001: 핵심 도메인 스키마 설계 (V2 마이그레이션)

- **상태**: 확정
- **날짜**: 2026-02-21
- **관련 파일**: `app/src/main/resources/db/migration/V2__core_tables.sql`

---

## Context

spec.md 기반으로 MVP를 구현하기 위한 최초 DB 스키마가 필요했다.
"완벽한 ERD보다 스펙을 구현하는 최소 테이블을 먼저" 원칙 하에 단일 마이그레이션(V2)으로 핵심 테이블 전체를 생성했다.

---

## Decision

### 테이블 구성 (16개)

| 그룹 | 테이블 |
|------|--------|
| 사용자 | `users`, `guests` |
| 상품/재고 | `products`, `inventory` |
| 체험 | `classes`, `slots` |
| 예약 | `bookings`, `booking_history` |
| 8회권 | `pass_purchases`, `pass_ledger` |
| 주문 | `orders`, `order_items`, `order_approvals`, `fulfillments`, `refunds` |
| 알림 | `notification_log` |

### 핵심 설계 선택

**낙관적 락 컬럼**
- `inventory.version`, `bookings.version` — `BIGINT DEFAULT 0`
- 단일 작품 중복 판매, 예약 동시 변경 방지용. JPA `@Version`과 매핑 예정.

**`refunds` 이중 FK**
- `order_id`, `booking_id` 모두 nullable FK로 설계.
- spec.md는 `order_id`만 명시하지만, 예약금 환불(booking deposit refund)도 동일 테이블에서 추적이 필요해 `booking_id`를 선제적으로 추가.
- 둘 중 하나는 반드시 non-null — 애플리케이션 레벨에서 강제.

**`slots` 복합 UNIQUE**
- `(class_id, start_at)` UNIQUE 제약 — 같은 클래스의 동일 시간 슬롯 중복 생성 방지.

**Enum → VARCHAR**
- 도메인 열거형(`OrderStatus`, `BookingStatus` 등)을 VARCHAR로 저장. JPA `@Enumerated(EnumType.STRING)`과 매핑.
- VARCHAR 길이는 실제 열거형 값 중 최대 길이 기준(`APPROVED_FULFILLMENT_PENDING` = 28자 → VARCHAR(30)).

**금액 단위**
- 원(KRW) 단위 `BIGINT`. 소수점 없음, DECIMAL 불필요.

**타임스탬프**
- `DATETIME(6)` — 마이크로초 정밀도. 시간 경계 정책(D-1 00:00, 1시간 전)의 정확한 판정에 필요.

---

## Alternatives

| 대안 | 기각 이유 |
|------|-----------|
| 도메인별 마이그레이션 분리 (V2~V10) | MVP 단계에서 과도한 분리. 테이블 간 FK 순서 관리가 복잡해짐. 초기엔 단일 파일이 리뷰와 롤백이 용이. |
| `refunds`를 `order_refunds` / `booking_refunds`로 분리 | 환불 재처리 배치가 단일 테이블 스캔으로 단순화 가능. 분리 시 배치 로직 복잡도 증가. |
| Enum 컬럼 타입 사용 | MySQL ENUM은 값 추가 시 ALTER TABLE 필요. VARCHAR + 애플리케이션 검증이 유연성↑. |
| `made_to_order_spec` 테이블 추가 | `docs/1Pager/0000_project_plan/plan.md` 명시 목록에 없고 MVP에서 즉시 필요하지 않음. 예약제작 구현 시 V3 이후 추가. |

---

## Consequences

**긍정**
- 단일 마이그레이션으로 전체 핵심 스키마 파악 가능 (가독성, 리뷰 용이).
- 낙관적 락 컬럼이 스키마 레벨에서 확보되어 동시성 구현 준비 완료.

**부정 / 후속 작업**
- `refunds`의 "둘 중 하나 non-null" 규칙이 DB 제약이 아닌 애플리케이션 로직에만 의존.
- `made_to_order_spec`, 비회원 인증 토큰 테이블 등은 별도 마이그레이션으로 추가 예정.
- `slots.booked_count` 갱신 경로: `confirmBooking()` 단일 트랜잭션 안에서 `SELECT FOR UPDATE` → `incrementBookedCount()` → save 순서로 확정 (→ ADR-0003).

---

## References

- `docs/PRD/0001_spec/spec.md` § 9 (데이터 모델)
- `docs/1Pager/0000_project_plan/plan.md` § 2.1, 2.2
- `domain/src/main/java/com/personal/happygallery/domain/` — 열거형 정의
