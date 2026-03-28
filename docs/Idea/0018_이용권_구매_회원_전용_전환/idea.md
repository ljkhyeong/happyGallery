# Idea-0018: 8회권 구매 회원 전용 전환

- 작성일: 2026-03-19
- 상태: 구현 완료

> **구현 완료** — V21 마이그레이션(`guest_id` 컬럼 제거)과 회원 전용 구매 전환이 완료되었다. 이 문서는 배경 기록으로만 유지한다.

---

## 배경

8회권(pass)은 90일 유효기간 동안 크레딧 차감/환불/만료 알림 등 지속적 상태 관리가 필요한 장기 상품이다.
현재 `pass_purchases` 테이블은 `guest_id` 또는 `user_id` 중 하나로 소유자를 식별하는 dual-owner 패턴을 사용하며,
이 구조가 다음과 같은 복잡도를 전파한다.

- 모든 pass 관련 쿼리에서 `WHERE user_id = ? OR guest_id = ?` 분기
- 비회원 8회권 구매 시 전화번호 인증 흐름 (`VerifiedGuestResolver`)
- 비회원 8회권을 회원으로 이전하는 guest claim 흐름
- 만료 리마인더 등 알림 대상 식별의 이중 경로

반면 주문(order)과 예약(booking)은 단건/단기 거래로 비회원 경로의 운영 부담이 상대적으로 적다.

## 결정

**8회권 구매를 회원 전용으로 전환한다.**

- 구매 엔드포인트: `POST /api/v1/me/passes` (기존 회원 경로) 단일화
- 비회원 구매 엔드포인트 제거: `POST /passes/guest`, `POST /passes/purchase`
- 프론트: `/passes/purchase` 페이지에서 비로그인 시 로그인 리다이렉트

## 후속 반영

- `pass_purchases.guest_id` 컬럼 제거
- `PassPurchase.guest` / `PassPurchase.claimToUser()` 제거
- `DefaultGuestClaimService`는 주문/예약 claim만 유지

## 영향 범위

| 영역 | 변경 |
|------|------|
| `PassController` | 삭제 (guest 전용 엔드포인트 2개) |
| `PassPurchaseUseCase` | `purchaseForGuest()`, `purchaseByPhone()` 제거 |
| `DefaultPassPurchaseService` | guest 메서드 + `VerifiedGuestResolver` 의존 제거 |
| `PassPurchase` | 신규 guest 구매 진입점 제거, 기존 guest 소유 데이터/claim 호환 유지 |
| `PassPurchasePage.tsx` | 회원 전용 전환, `AuthGateModal` 제거 |
| PRD-0002 §6.3 | "비회원 8회권 구매는 guest 경로를 유지한다" → 회원 전용 |
| API 계약 (PRD-0004) | guest 8회권 섹션 제거 |

## 근거

- 8회권은 장기 관리가 필요한 상품이므로 회원 식별이 본질적으로 중요하다
- member store 전환(PRD-0002) 방향과 일치한다
- 비회원 8회권 구매 후 claim하는 2단계 흐름보다 처음부터 회원으로 구매하는 1단계가 사용자 경험에서도 낫다
- 주문/예약의 비회원 경로는 유지하므로 서비스 진입 장벽은 그대로다
