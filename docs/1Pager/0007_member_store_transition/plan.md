# Member Store Transition Plan

현재 구현된 비회원 중심 플로우를 "회원 스토어 + 비회원 보조 경로" 구조로 전환하기 위한 상위 계획이다.

현재 상태:
- `U1` 완료
- `U2`, `U3`, `U4` 완료
- `U5` 완료 (`/my`, guest claim, 회원 예약 상세/변경·취소, `/guest/**` 분리)
- `U6` 2차 완료 (rollout 기준 + Playwright smoke 1~8 + guest claim browser automation)
- 남은 후속은 `P8-3` flaky 안정화와 legacy redirect alias 제거 판단

기준 문서:
- 현재 구현 기준: `HANDOFF.md`, `docs/PRD/0001_spec/spec.md`
- 차기 요구사항 초안: `docs/PRD/0002_member_store_transition/spec.md`

---

## 1. 목표

- 네이버 스마트스토어처럼 상품 상세가 구매 중심 화면이 되도록 고객 프론트를 재구성한다.
- 회원은 로그인 후 주문/예약/8회권 조회를 추가 인증 없이 처리할 수 있게 한다.
- 비회원은 기존 휴대폰 인증/토큰 기반 조회를 유지하되, 브라우징 단계에서는 인증을 강제하지 않는다.
- 예약/8회권/주문의 "인증 시점"을 첫 화면이 아니라 제출 직전으로 옮긴다.

---

## 2. 핵심 원칙

- 브라우징과 확정은 분리한다. 상품/클래스/시간 탐색은 무인증, 결제·예약 확정만 인증이 필요하다.
- 회원과 비회원은 공존한다. 기존 guest API를 즉시 제거하지 않는다.
- 회원 조회와 비회원 조회는 분리한다. 회원은 `내 정보` 경로, 비회원은 휴대폰 인증/토큰 경로를 사용한다.
- 상품 상세가 주문 메인 진입점이 된다. 현재 `/orders/new`는 보조 경로나 레거시 fallback으로 내린다.
- 고객 인증은 관리자 인증과 분리한다. 관리자 세션 저장소나 `X-Admin-Key` 패턴을 재사용하지 않는다.
- 현재 구현과 미래 요구사항을 분리하기 위해, 차기 요구사항은 `docs/PRD/0002_member_store_transition/spec.md`에 먼저 적고 구현 후 `0001_spec`에 흡수한다.

---

## 3. 작업 단위 요약

| 단위 | 성격 | 선행 | 병렬 가능 | 핵심 산출물 |
|------|------|------|-----------|-------------|
| `U1` | 백엔드 중심 | 없음 | 낮음 | 고객 회원가입/로그인/세션 기반 |
| `U2` | 백엔드 중심 | `U1` | 중간 | 주문/예약/8회권의 회원/비회원 이중 계약 |
| `U3` | 프론트 중심 | `U1` 일부 | 중간 | 상점형 IA, 상품 상세 구매 패널 |
| `U4` | 프론트+백엔드 | `U1`, `U2` | 중간 | 제출 직전 인증 게이트, guest checkout |
| `U5` | 프론트+백엔드 | `U2` | 중간 | 회원 마이페이지, 비회원 조회 분리 |
| `U6` | 통합/운영 | `U1`~`U5` | 낮음 | 데이터 이관 규칙, E2E, rollout 기준 |

각 단위의 상세 플랜:
- `docs/1Pager/0007_member_store_transition/01_customer_auth_foundation.md`
- `docs/1Pager/0007_member_store_transition/02_identity_model_and_contracts.md`
- `docs/1Pager/0007_member_store_transition/03_storefront_and_product_detail.md`
- `docs/1Pager/0007_member_store_transition/04_deferred_verification_and_checkout.md`
- `docs/1Pager/0007_member_store_transition/05_member_self_service_and_guest_lookup.md`
- `docs/1Pager/0007_member_store_transition/06_rollout_migration_and_e2e.md`

---

## 4. 권장 순서

1. `U1` 고객 인증 기반을 먼저 확정한다.
2. `U2` 회원/비회원 이중 계약을 백엔드에 깔아둔다.
3. `U3` 상점형 IA와 상품 상세 구매 UX를 붙인다.
4. `U4`, `U5`를 병렬로 진행한다.
5. `U6`에서 guest 이력 이관 규칙, E2E, rollout 기준을 마무리한다.

---

## 5. 병렬 작업 가이드

- 백엔드 에이전트 1: `U1`
- 백엔드 에이전트 2: `U2` 준비용 API/도메인 영향 분석
- 프론트 에이전트 1: `U3`
- 프론트 에이전트 2: `U4`용 공통 인증 게이트/모달 구조 탐색
- 통합 에이전트: `U6`의 테스트 매트릭스/rollout 문서 초안

단, `U2`의 API 계약이 확정되기 전에는 `U4`, `U5` 구현 커밋을 크게 밀지 않는다.

---

## 6. 선결정 사항

- 회원 인증 권장안: `HttpOnly` 쿠키 기반 고객 세션
- 세션 저장 권장안: `user_session` 테이블 또는 이에 준하는 지속 저장소
- 자동 guest → user 병합은 금지
- guest 이력 연결은 로그인 후 휴대폰 인증으로 `claim`하는 별도 흐름으로 설계
- 범위 밖:
  - 장바구니
  - 리뷰/평점/Q&A
  - 소셜 로그인
  - 네이버페이/스마트스토어 실연동
  - 쿠폰/적립금/찜 기능

---

## 7. 완료 기준

- 회원은 로그인 후 `내 주문`, `내 예약`, `내 8회권`을 추가 인증 없이 확인할 수 있다.
- 비회원은 상품/클래스/시간 탐색 후 제출 직전에만 휴대폰 인증 또는 guest 진행을 고를 수 있다.
- 상품 상세 페이지에서 바로 주문 흐름으로 진입할 수 있다.
- `0002_member_store_transition` PRD와 실제 구현, `HANDOFF.md`가 서로 어긋나지 않는다.
