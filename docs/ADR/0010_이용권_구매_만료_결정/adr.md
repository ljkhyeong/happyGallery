# ADR-0010: 8회권 구매/만료 구현 결정

- **날짜**: 2026-02-26
- **상태**: 승인됨
- **관련 섹션**: `docs/PRD/0001_기준_스펙/spec.md` §7.1

---

## 컨텍스트

8회권(Pass/Credit) MVP 첫 단계로, 구매 생성과 만료 소멸 기능을 구현한다.
기존에 `pass_purchases`, `pass_ledger` 테이블(V2 마이그레이션)과 `PassLedgerType` enum이 존재하며,
엔티티/Repository/서비스만 신규 작성하면 된다.

---

## 결정 사항

### 0. 구매 시점에 이용권 계획을 스냅샷으로 저장

**결정**: 신규 구매는 `PassPlan.REGULAR_CRAFT_8`로 확정하고 `pass_purchases.plan_code`에 저장한다.
계획은 표시 이름과 사용 가능 클래스 정책을 함께 가진 판매 계약이다. 정책을 바꿀 때 기존 enum 상수의 의미를
수정하지 않고 새 계획 코드를 추가한다. 정책 도입 전 구매 건은 `LEGACY_ALL_CLASSES`로 이관한다.

**이유**:
- 판매 뒤 클래스 정책이 바뀌어도 구매 당시 계약을 재현할 수 있다.
- 목록·상세 응답의 `planCode`, `planName`과 예약 시 사용 가능 여부를 같은 값에서 만든다.
- 향수 원데이 클래스와 관리자가 `passEligible=false`로 지정한 클래스에는 `REGULAR_CRAFT_8`을 사용할 수 없다.

### 1. `expires_at` 저장 타입: `LocalDateTime`

**결정**: DB 컬럼(`DATETIME(6)`)과 동일하게 `LocalDateTime` 사용. `expires_at`은 마지막 사용 가능일 다음날 00:00 KST를 나타내는 exclusive 만료 경계로 저장한다.

**이유**:
- 기존 `Booking`, `Slot` 등 도메인 엔티티가 모두 `LocalDateTime` 사용 — 일관성 유지
- 계산은 `TimeBoundary.passExpiresAt(ZonedDateTime)` → `.toLocalDateTime()` 변환으로 결제일 포함 90일의 다음날 00:00 KST를 보장
- `purchased_at`도 DB UTC 기본값에 맡기지 않고 구매 트랜잭션의 서울 현지시각을 명시해 매출 날짜와 고객 표시 시각을 같은 기준으로 유지한다.

**위험**: 서버 타임존이 Asia/Seoul이 아닌 환경에서는 계산 오차 가능. `TimeBoundary`가 입력 시각을 `Clocks.SEOUL` 날짜로 변환해 계산하므로 현 구성에서는 안전.

---

### 2. `purchased_at`: 구매 트랜잭션 시각 명시 저장

**결정**: `purchased_at` 컬럼은 `updatable=false`로 두고, 구매 서비스가 사용하는 `Clock`의 서울 현지시각을 생성 팩토리에 전달해 명시 저장한다.

**이유**: 이용권 매출은 고객에게 표시되는 결제일과 같은 KST 날짜 경계로 집계해야 한다. DB 기본값에 맡기면 UTC로 생성되는 `created_at` 계열과 섞여 오후 시간대 매출이 다음 날로 분류될 수 있다.

**위험**: 애플리케이션 노드 간 시계 차이가 결제 시각에 반영될 수 있다. 모든 노드는 NTP로 동기화하고, 동일한 `Clock`에서 `purchasedAt`과 `expiresAt`을 함께 계산한다.

---

### 3. 만료 처리: 쓰기 경로 즉시 정규화 + 배치 보완

**결정**: `PassExpiryBatchService.expireAll()`을 `POST /admin/passes/expire` HTTP 엔드포인트로 노출하고,
운영 스케줄러에도 연결한다. 수동 트리거와 정기 실행을 모두 허용한다. 다만 배치는 정합성의 전제 조건이 아니다.
크레딧 복구와 전체 환불은 `pass_purchases` 행을 잠근 뒤 현재 시각과 `expires_at`을 다시 비교하고,
만료됐는데 잔여 크레딧이 있으면 해당 트랜잭션에서 `EXPIRE` 원장과 잔액 0을 함께 반영한다.

**이유**:
- 운영자가 수동 검증과 긴급 실행을 할 수 있어야 한다
- 실제 운영에서는 정기 실행이 빠지면 만료 소멸과 알림이 누락된다
- 다른 배치와 동일한 로깅/AOP 규약으로 묶을 수 있다
- 배치 지연 중 들어온 취소나 환불 요청도 만료 크레딧을 복구·환불하지 않아야 한다

**위험**: 수동 트리거와 스케줄러가 같은 날 중복 실행될 수 있으므로, 반환값은 `BatchResult`로 표준화하고 중복 발송/처리 여부를 건수로 관찰 가능하게 한다.

**구현 메모**:
- `expires_at <= now`이고 `remaining_credits > 0`인 이용권을 만료 배치 대상으로 본다.

---

### 4. `EARN` ledger: 회원 구매 생성 시점에 즉시 기록

**결정**: 결제 confirm이 회원 8회권 구매를 확정하면 `purchaseForMember()`가 `PassPurchase`를 저장하고 같은 트랜잭션에서 `PassLedger(EARN, totalCredits)`를 기록한다.

**이유**: "크레딧이 돈이다" 원칙 — 크레딧 잔액 변동은 반드시 ledger 기록이 선행 또는 동반되어야 한다.

---

### 5. `EXPIRE` ledger: 잔액 소멸과 같은 트랜잭션에서 기록

**결정**: 잠긴 `PassPurchase`가 만료 경계에 도달했고 잔여 크레딧이 있을 때만 소멸 수량을 확정하고,
`EXPIRE` 원장 저장과 `remaining_credits=0`을 같은 트랜잭션에서 커밋한다.

**이유**: 소멸 수량을 정확히 남기면서 원장과 잔액의 부분 반영을 막는다. 모든 경로가 같은 행 잠금을 사용하고
첫 처리에서 잔액을 0으로 만들기 때문에 배치·취소·환불이 경합하거나 재실행돼도 `EXPIRE` 원장은 중복 생성되지 않는다.

---

### 6. 만료 7일 전 알림: 구매한 8회권별 정확히 1회

**결정**: 만료 임박 알림은 아직 만료되지 않았고 7일 이내 만료되는 건을 대상으로 하고,
`PASS_EXPIRY_SOON + PASS_PURCHASE + passId`가 같은 outbox가 이미 있으면 재요청하지 않는다.

**이유**:
- "7일 전 알림"을 7일 동안 반복 발송하지 않기 위해서
- 수동 트리거와 정기 스케줄이 함께 있어도 같은 날 중복 발송을 막아야 하기 때문
- 같은 회원이 여러 8회권을 구매했으면 각 구매 건의 만료를 별도로 안내해야 하기 때문

**구현 메모**:
- 대상 범위: `(now, now+7d]`
- 중복 방지: 사용자 성공 로그나 멱등키 문자열 형식이 아니라 `notification_outbox(event_type, aggregate_type, aggregate_id)`를 기준으로 한다.
- 배치 전체를 하나의 트랜잭션으로 묶지 않는다. 각 알림 outbox 저장 실패는 해당 8회권 실패로 집계하고 다른 구매 건은 계속 처리한다.
- outbox를 선점한 뒤에도 같은 범위와 `remaining_credits > 0`을 다시 조회한다. 그사이 만료·소진·환불되어
  발송 조건을 충족하지 않으면 외부 채널을 호출하지 않고 outbox를 `OBSOLETE`로 종결한다.

---

## 결과

| 파일 | 모듈 | 역할 |
|------|------|------|
| `PassPurchase.java` | domain | pass_purchases 엔티티, `expire()` |
| `PassLedger.java` | domain | pass_ledger 엔티티 (append-only) |
| `PassPurchaseRepository.java` | infra | 만료/알림 대상 쿼리 |
| `PassLedgerRepository.java` | infra | CRUD |
| `DefaultPassPurchaseService.java` | application | 회원 구매 생성과 `EARN` 원장 기록 |
| `DefaultPassExpiryBatchService.java` | application | 만료 처리 + 알림 대상 조회 |
| `PaymentController.java` | adapter-in-web | `POST /api/v1/payments/prepare`, `POST /api/v1/payments/confirm` |
| `AdminPassController.java` | adapter-in-web | `POST /api/v1/admin/passes/expire` |
| `PassPurchaseUseCaseIT.java` | application-test | 구매·만료 통합 흐름 검증 |

---

## 후속 반영

- `/admin/**`는 Spring Security 관리자 체인과 Redis-backed Bearer 세션으로 보호한다.
- ~~`POST /passes/guest` — 현재 `guestId` 직접 수신. 실제 서비스에서는 인증(JWT/세션) 또는 전화 인증 후 guestId 발급 흐름 필요~~ → 아래 Update 참조

---

## Update (2026-03-19)

8회권 구매를 회원 전용으로 전환했다 (Idea-0018).

- guest 구매 엔드포인트 제거: `POST /passes/guest`, `POST /passes/purchase`
- 당시 회원 구매 엔드포인트로 단일화: `POST /api/v1/me/passes` (2026-04-26 결제 API 도입으로 아래 Update 기준으로 대체)
- 신규 guest 구매 진입점은 제거
- guest 소유 8회권 상태와 claim 흐름도 함께 제거

## Update (2026-04-26)

결제 진입점 도입으로 회원 8회권 구매 생성 경로를 다시 일원화했다.

- 구매 생성: `POST /api/v1/payments/prepare` (`context=PASS`) → `POST /api/v1/payments/confirm`
- `GET /api/v1/me/passes`, `GET /api/v1/me/passes/{id}`는 회원 8회권 조회 전용으로 유지한다.
- 가격은 클라이언트가 보내지 않고 서버 설정 `app.pass.total-price` (`PASS_TOTAL_PRICE`, 기본 240000)으로 확정한다.

## Update (2026-07-21)

- 신규 구매의 계획은 `REGULAR_CRAFT_8`로 고정하고 `plan_code`에 스냅샷으로 저장한다.
- 회원 8회권 목록·상세는 `planCode`, `planName`과 환불 진행 상태를 함께 반환한다.
- 정규 공예 8회권은 `passEligible=true`이면서 카테고리가 `PERFUME`가 아닌 클래스에만 사용할 수 있다.
