---
name: domain-state-machine
description: >
  Workflow for state machine transitions, guard methods, and domain enum changes in the happyGallery
  backend. Always use this skill when the request involves: 주문 상태 전이 규칙 변경, 예약 상태 전이 추가,
  enum 값 추가/제거, 가드 메서드 (requireApprovable, requireCancellable 등) 추가, 상태 전이가 거부되는 로직,
  "이 상태에서 취소가 안 돼요" 같은 비즈니스 규칙 오류, OrderStatus, BookingStatus, BalanceStatus,
  BookingHistoryAction, PassLedgerType, ProductType, ProductStatus 열거형 변경, 또는 서비스 코드에
  흩어진 status 체크를 enum 가드로 리팩토링. Also use this skill when someone asks
  "왜 이 상태에서 승인이 안 돼요?" / "새 주문 상태를 추가하고 싶어요" / "가드 메서드 패턴이 뭔가요?" /
  "서비스에 status 체크 로직이 너무 흩어져 있어요". Use this skill alongside the domain skill (booking,
  order, pass) when both the transition rule AND the domain service need updating together.
---

# happyGallery Domain State Machine

## Core references

- Use `docs/PRD/0001_기준_스펙/spec.md` for allowed transitions and business rules per status.
- Read the needed ADRs:
  - `docs/ADR/0002_state-transition-guard/adr.md`
  - `docs/ADR/0013_주문_승인_모델/adr.md`
  - `docs/ADR/0014_예약_제작_주문_결정/adr.md`

## Domain enums and their location

| 도메인 | 열거형 | 위치 |
|--------|--------|------|
| order | `OrderStatus`, `FulfillmentType`, `RefundStatus` | `domain/.../domain/order/` |
| booking | `BookingStatus`, `BalanceStatus`, `BookingHistoryAction` | `domain/.../domain/booking/` |
| pass | `PassLedgerType` | `domain/.../domain/pass/` |
| product | `ProductType`, `ProductStatus` | `domain/.../domain/product/` |

## Cross-cutting rules

`happygallery-spring-backend`의 Respect module boundaries·Repository constraints·Test writing rules, `api-contract`의 Non-negotiable invariants, `happygallery-test-refactor`의 Assertion conventions를 이 스킬에서도 항상 따른다.

## Non-negotiable invariants

- **Always use guard methods** — never check `status.equals(X)` inline in service code. Instead call `status.requireApprovable()`, `status.requireCancellable()`, etc.
- Guard methods throw a domain exception with a clear error code when the transition is illegal.
- When adding a new valid transition, add the guard method to the enum and update the spec and relevant ADR.
- Do not add transition logic directly in controllers or repositories.
- When adding a new enum value, check all switch/when expressions and guard methods that need updating.

## Guard method pattern

```java
// enum 안에 가드를 집중
public enum OrderStatus {
    PENDING, APPROVED, REJECTED, ...;

    public void requireApprovable() {
        if (this != PENDING) {
            throw new DomainException(ErrorCode.ORDER_NOT_APPROVABLE, this);
        }
    }
}

// 서비스에서는 가드만 호출
order.getStatus().requireApprovable();
order.approve(...);
```

## Likely code locations

- `domain/src/main/java/com/personal/happygallery/domain/order/` — OrderStatus, FulfillmentType, RefundStatus
- `domain/src/main/java/com/personal/happygallery/domain/booking/` — BookingStatus, BalanceStatus, BookingHistoryAction
- `domain/src/main/java/com/personal/happygallery/domain/pass/` — PassLedgerType
- `domain/src/main/java/com/personal/happygallery/domain/product/` — ProductType, ProductStatus
- `app/src/main/java/com/personal/happygallery/app/order/OrderApprovalService.java`
- `app/src/main/java/com/personal/happygallery/app/booking/`

## High-value tests (for reference)

- `app/src/test/java/com/personal/happygallery/policy/OrderStatusTransitionPolicyTest.java`
- `app/src/test/java/com/personal/happygallery/policy/TimeBoundaryPolicyTest.java`

## Verification workflow

- Guard method or enum transition changes: `./gradlew :app:policyTest`
- Use case-level state machine changes: `./gradlew --no-daemon :app:useCaseTest --tests "*Order*"` or `--tests "*Booking*"` or `--tests "*Pass*"` depending on the domain

## Doc sync checklist

- State transition rules and valid status values: `docs/PRD/0001_기준_스펙/spec.md`
- Guard method design decisions: `docs/ADR/0002_state-transition-guard/adr.md`
- Session status and remaining work: `HANDOFF.md`
