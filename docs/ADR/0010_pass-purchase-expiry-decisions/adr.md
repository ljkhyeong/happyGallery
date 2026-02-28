# ADR-0010: 8회권 구매/만료 구현 결정

- **날짜**: 2026-02-26
- **상태**: 승인됨
- **관련 섹션**: §7.1 (PLAN.md)

---

## 컨텍스트

8회권(Pass/Credit) MVP 첫 단계로, 구매 생성과 만료 소멸 기능을 구현한다.
기존에 `pass_purchases`, `pass_ledger` 테이블(V2 마이그레이션)과 `PassLedgerType` enum이 존재하며,
엔티티/Repository/서비스만 신규 작성하면 된다.

---

## 결정 사항

### 1. `expires_at` 저장 타입: `LocalDateTime`

**결정**: DB 컬럼(`DATETIME(6)`)과 동일하게 `LocalDateTime` 사용. 시간대 변환 없이 서울 시간으로 계산해 저장.

**이유**:
- 기존 `Booking`, `Slot` 등 도메인 엔티티가 모두 `LocalDateTime` 사용 — 일관성 유지
- 계산은 `TimeBoundary.passExpiresAt(ZonedDateTime)` → `.toLocalDateTime()` 변환으로 서울 시간 기준 90일 보장

**위험**: 서버 타임존이 Asia/Seoul이 아닌 환경에서는 계산 오차 가능. `Clock`이 항상 `Clocks.SEOUL` 기반이므로 현 구성에서는 안전.

---

### 2. `purchased_at`: DB DEFAULT 위임

**결정**: `purchased_at` 컬럼을 `insertable=false, updatable=false`로 선언. DB `DEFAULT CURRENT_TIMESTAMP(6)` 자동 기록.

**이유**: 기존 `Booking.createdAt`과 동일한 패턴. 애플리케이션에서 별도로 주입하지 않으면 클록 동기화 문제 없음.

**위험**: 서비스 레이어에서 `purchasedAt`을 참조해 `expiresAt`을 계산할 수 없으므로, `expiresAt`은 `ZonedDateTime.now(clock)`로 직접 계산해 저장.

---

### 3. 만료 배치: 서비스 메서드 직접 호출 (스케줄러 미적용)

**결정**: `PassExpiryBatchService.expireAll()`을 `POST /admin/passes/expire` HTTP 엔드포인트로 노출. 스프링 `@Scheduled` 미사용.

**이유**:
- §7.1 MVP 범위: "자동 처리 가능" = 배치 실행 가능 여부 검증이 목표
- `@Scheduled`는 §9 알림/배치 섹션에서 통합 구현 예정
- 운영자가 수동 트리거로 동작 검증 가능

**위험**: 스케줄러가 없으므로 만료가 자동으로 일어나지 않음. 프로덕션 배포 전 스케줄러 연결 필수 (→ §9).

---

### 4. `EARN` ledger: 구매 시점에 즉시 기록

**결정**: `purchaseForGuest()` 내부에서 `PassPurchase` 저장 직후 `PassLedger(EARN, 8)` 기록.

**이유**: "크레딧이 돈이다" 원칙 — 크레딧 잔액 변동은 반드시 ledger 기록이 선행 또는 동반되어야 한다.

---

### 5. `EXPIRE` ledger: `expire()` 호출 전에 기록

**결정**: `expireAll()` 내부에서 `pass.expire()`(remaining=0) 전에 EXPIRE ledger를 먼저 저장.

**이유**: ledger의 `amount` 필드에 소멸된 크레딧 수를 정확히 기록하기 위해 `getRemainingCredits()` 호출 순서 보장 필요.

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

- 스케줄러(`@Scheduled`) 연결 → §9 알림/배치
- `/admin/**` 인증 미적용 → §11
- `POST /passes/guest` — 현재 `guestId` 직접 수신. 실제 서비스에서는 인증(JWT/세션) 또는 전화 인증 후 guestId 발급 흐름 필요
