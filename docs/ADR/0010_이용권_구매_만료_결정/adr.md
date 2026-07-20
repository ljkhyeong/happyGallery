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

### 3. 만료 배치: 스케줄러 연결 + 수동 트리거 병행

**결정**: `PassExpiryBatchService.expireAll()`을 `POST /admin/passes/expire` HTTP 엔드포인트로 노출하고,
운영 스케줄러에도 연결한다. 수동 트리거와 정기 실행을 모두 허용한다.

**이유**:
- 운영자가 수동 검증과 긴급 실행을 할 수 있어야 한다
- 실제 운영에서는 정기 실행이 빠지면 만료 소멸과 알림이 누락된다
- 다른 배치와 동일한 로깅/AOP 규약으로 묶을 수 있다

**위험**: 수동 트리거와 스케줄러가 같은 날 중복 실행될 수 있으므로, 반환값은 `BatchResult`로 표준화하고 중복 발송/처리 여부를 건수로 관찰 가능하게 한다.

**구현 메모**:
- `expires_at <= now`이고 `remaining_credits > 0`인 이용권을 만료 배치 대상으로 본다.

---

### 4. `EARN` ledger: 구매 시점에 즉시 기록

**결정**: `purchaseForGuest()` 내부에서 `PassPurchase` 저장 직후 `PassLedger(EARN, 8)` 기록.

**이유**: "크레딧이 돈이다" 원칙 — 크레딧 잔액 변동은 반드시 ledger 기록이 선행 또는 동반되어야 한다.

---

### 5. `EXPIRE` ledger: `expire()` 호출 전에 기록

**결정**: `expireAll()` 내부에서 `pass.expire()`(remaining=0) 전에 EXPIRE ledger를 먼저 저장.

**이유**: ledger의 `amount` 필드에 소멸된 크레딧 수를 정확히 기록하기 위해 `getRemainingCredits()` 호출 순서 보장 필요.

---

### 6. 만료 7일 전 알림: 구매한 8회권별 정확히 1회

**결정**: 만료 임박 알림은 `expires_at`이 `오늘 + 7일`인 건만 대상으로 하고,
`recipient + PASS_EXPIRY_SOON + PASS_PURCHASE + passId` outbox 멱등키가 이미 있으면 재요청하지 않는다.

**이유**:
- "7일 전 알림"을 7일 동안 반복 발송하지 않기 위해서
- 수동 트리거와 정기 스케줄이 함께 있어도 같은 날 중복 발송을 막아야 하기 때문
- 같은 회원이 여러 8회권을 구매했으면 각 구매 건의 만료를 별도로 안내해야 하기 때문

**구현 메모**:
- 대상 범위: `[targetStart, targetEnd)` where `targetStart = today+7d 00:00`
- 중복 방지: 사용자 성공 로그가 아니라 `notification_outbox.idempotency_key`를 기준으로 한다.
- 배치 전체를 하나의 트랜잭션으로 묶지 않는다. 각 알림 outbox 저장 실패는 해당 8회권 실패로 집계하고 다른 구매 건은 계속 처리한다.

---

## 결과

| 파일 | 모듈 | 역할 |
|------|------|------|
| `PassPurchase.java` | domain | pass_purchases 엔티티, `expire()` |
| `PassLedger.java` | domain | pass_ledger 엔티티 (append-only) |
| `PassPurchaseRepository.java` | infra | 만료/알림 대상 쿼리 |
| `PassLedgerRepository.java` | infra | CRUD |
| `PassPurchaseService.java` | app | 구매 생성 |
| `PassExpiryBatchService.java` | app | 만료 처리 + 알림 대상 조회 |
| `PassController.java` | app | `POST /passes/guest` |
| `AdminPassController.java` | app | `POST /admin/passes/expire` |
| `PassPurchaseUseCaseIT.java` | app-test | 5개 통합 테스트 (전체 통과) |

---

## 미해결 과제

- `/admin/**` 인증 미적용 → §11
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
