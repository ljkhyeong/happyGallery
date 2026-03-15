# spec.md
회원 스토어 전환 요구사항 초안

상태:
- U1 구현 완료 (고객 인증 기반)
- U2, U3, U4 구현 완료
- U5 구현 완료 (회원 `/api/v1/me/**`, `/my`, guest claim preview/verify/claim, 회원 예약 상세/변경·취소, `/guest/**` canonical route)
- U6 2차 완료 (rollout 기준 정리, Playwright smoke 1~8 통과, guest claim browser automation 반영)
- 현재 구현 기준 문서는 `docs/PRD/0001_spec/spec.md`
- 이 문서는 "차기 회원 스토어 전환" 요구사항을 다른 에이전트가 병렬 구현하기 위한 목표 문서다

참고 레퍼런스:
- 사용자가 제시한 네이버 스마트스토어 상품 상세 페이지 레퍼런스
  - `https://smartstore.naver.com/ballon2/products/12510354216?...`

---

## 0. 문서 목적

- 현재 비회원 중심 프론트를 회원 스토어 구조로 전환하기 위한 목표 상태를 정리한다.
- 다른 에이전트가 백엔드/프론트/문서를 병렬로 나눠 작업할 수 있게 범위를 자른다.
- 아직 구현되지 않은 요구사항을 현재 구현 문서와 분리해 관리한다.

---

## 1. 목표 경험

- 사용자는 스토어 홈과 상품 상세를 중심으로 상품을 탐색한다.
- 상품 상세 페이지에서 바로 주문 흐름으로 진입할 수 있다.
- 회원은 로그인 후 주문/예약/8회권 조회를 추가 인증 없이 처리한다.
- 비회원은 기존처럼 휴대폰 인증 또는 토큰 기반 조회를 유지한다.
- 예약, 8회권 구매, guest 주문은 제출 직전까지 인증을 요구하지 않는다.

---

## 2. 사용자 유형

### 2.1 회원

- 이메일+비밀번호 기반 계정을 가진 사용자
- 로그인 세션으로 `내 주문`, `내 예약`, `내 8회권`에 접근 가능
- 예약/구매 제출 시 휴대폰 인증을 매번 다시 요구하지 않음

### 2.2 비회원

- 계정 없이 guest checkout을 사용하는 사용자
- 주문/예약 생성 시 휴대폰 인증 또는 주문/예약 토큰이 필요
- 조회는 비회원 전용 경로로만 접근 가능

### 2.3 관리자

- 기존 관리자 인증 모델 유지
- 고객 회원 인증과 완전히 분리

---

## 3. 인증 정책

### 3.1 고객 회원 인증

- 고객 회원가입 API와 로그인 API를 제공한다.
- 권장 구현은 `HttpOnly` 쿠키 기반 세션이다.
- 고객 세션은 관리자 세션과 분리한다.
- 세션 만료 정책은 별도 문서화하되, 브라우저 새로고침으로 쉽게 끊기지 않는 수준을 기본으로 한다.

### 3.2 비회원 인증

- guest checkout은 유지한다.
- 주문/예약 조회는 현재와 같은 token 또는 휴대폰 인증 기반을 유지한다.
- 비회원 생성 플로우에서만 휴대폰 인증을 사용한다.

### 3.3 인증 시점

- 상품/클래스/시간 탐색 단계에서는 인증을 요구하지 않는다.
- 결제/예약 확정 직전에만 로그인/회원가입/비회원 진행을 선택하게 한다.

---

## 4. 정보 구조(IA)

### 4.1 공개 경로

- `/`
- `/products`
- `/products/:id`
- `/bookings/new`
- `/passes/purchase`
- `/login`
- `/signup`

### 4.2 회원 UI 경로

- `/login`
- `/signup`
- `/my`
- `/my/orders/:id`
- `/my/bookings/:id`

### 4.3 회원 API 경로

- `/api/v1/me/orders`
- `/api/v1/me/bookings`
- `/api/v1/me/passes`

### 4.4 비회원 경로

- 현재 구현:
  - `/orders/new` (`productId`, `qty` query prefill 지원)
  - `/guest/orders`
  - `/guest/bookings`
  - `/orders/detail` -> `/guest/orders`
  - `/bookings/manage` -> `/guest/bookings`

레거시 경로:
- `/orders/new` 는 guest 주문 생성 fallback으로 유지한다.
- `/orders/detail`, `/bookings/manage` 는 구현 전환 과정에서 redirect alias로 유지할 수 있으나, 최종적으로는 제거 여부를 별도 판단한다.

---

## 5. 화면 요구사항

### 5.1 홈

- 기존 기능 카드 모음 대신 스토어 진입 화면으로 재구성한다.
- 상품, 체험 예약, 8회권, 내 정보 또는 로그인 진입점이 명확해야 한다.

### 5.2 상품 상세

- 상품 상세는 주문 메인 진입점이다.
- 필수 표시:
  - 상품명
  - 가격
  - 재고/구매 가능 상태
  - 상품 유형
  - 수량 선택
  - 총액
  - 구매 버튼
- 모바일에서는 구매 CTA가 쉽게 노출되어야 한다.

### 5.3 예약 생성

- 먼저 클래스/날짜/시간을 고른다.
- 제출 직전에 인증 게이트를 연다.
- 회원이면 바로 제출, 비회원이면 휴대폰 인증 후 제출한다.

### 5.4 8회권 구매

- 먼저 상품 안내/가격을 확인한다.
- 제출 직전에 인증 게이트를 연다.

### 5.5 조회 화면

- 회원은 `내 주문`, `내 예약`, `내 8회권`에서 조회한다.
- 회원 예약 상세 화면에서 변경/취소를 직접 수행한다.
- 비회원은 비회원 전용 조회 화면에서만 조회한다.

---

## 6. 비즈니스 규칙 변경

### 6.1 주문

- 회원 주문은 로그인 세션 기준으로 생성한다.
- 비회원 주문은 guest checkout과 휴대폰 인증을 거쳐 생성한다.
- 상품 상세에서 guest 주문 fallback으로 이동할 때는 선택한 상품과 수량을 prefill 한다.
- 회원 주문 조회는 추가 token 없이 가능하다.
- 비회원 주문 조회는 token 또는 비회원 인증이 필요하다.

### 6.2 예약

- 회원 예약은 로그인 상태에서 생성/조회/변경/취소한다.
- 비회원 예약은 기존 guest 인증 경로를 유지한다.
- 예약 시간 탐색에는 인증이 필요 없다.

### 6.3 8회권

- 회원 8회권은 로그인 기준으로 구매/조회한다.
- 비회원 8회권 구매는 guest 경로를 유지한다.

---

## 7. 데이터 모델 목표 상태

### 7.1 users

- 기존 `users` 테이블을 실제 고객 회원 모델로 사용한다.
- 보강 후보:
  - `phone_verified`
  - `last_login_at`

### 7.2 sessions

- 고객 세션 저장 구조가 필요하다.
- 권장안:
  - `user_sessions`
  - `user_id`
  - `session_token_hash`
  - `expires_at`
  - `created_at`

### 7.3 주문/예약/8회권 식별

- `orders`, `bookings`, `pass_purchases` 는 `user_id` 또는 `guest_id` 중 하나만 가진다.
- 둘 다 비어 있거나 둘 다 채워진 상태는 금지한다.

### 7.4 guest 이력 claim

- 자동 병합 금지
- 권장안:
  - 로그인한 회원이 휴대폰 인증을 통과한 뒤 기존 guest 이력을 수동으로 claim

---

## 8. API 목표 상태

### 8.1 고객 인증 (U1 구현 완료)

- `POST /api/v1/auth/signup` — 이메일/비밀번호/이름/전화번호, 201 + HttpOnly 쿠키
- `POST /api/v1/auth/login` — 이메일/비밀번호, 200 + HttpOnly 쿠키
- `POST /api/v1/auth/logout` — 204 + 쿠키 삭제
- `GET /api/v1/me` — 로그인 필수, 200 `{id, email, name, phone, phoneVerified}`
- 세션: `user_sessions` 테이블 (SHA-256 해시 저장), 7일 TTL
- 쿠키: `HG_SESSION`, HttpOnly, SameSite=Lax, Path=/

### 8.2 회원 전용 조회 / 생성

- `GET /api/v1/me/orders`
- `GET /api/v1/me/orders/{id}`
- `POST /api/v1/me/orders`
- `GET /api/v1/me/bookings`
- `GET /api/v1/me/bookings/{id}`
- `POST /api/v1/me/bookings`
- `PATCH /api/v1/me/bookings/{id}/reschedule`
- `DELETE /api/v1/me/bookings/{id}`
- `GET /api/v1/me/passes`
- `GET /api/v1/me/passes/{id}`
- `POST /api/v1/me/passes`
- `GET /api/v1/me/guest-claims/preview`
- `POST /api/v1/me/guest-claims/verify`
- `POST /api/v1/me/guest-claims`

### 8.3 비회원 조회 유지

- 기존 주문/예약 token 조회 API 유지
- 프론트 canonical route는 `/guest/orders`, `/guest/bookings`를 사용한다.

### 8.4 생성 API

- 주문, 예약, 8회권 구매 API는 로그인 사용자와 guest payload를 모두 지원해야 한다.
- 로그인 사용자가 있으면 `user_id`를 우선 사용한다.

---

## 9. Rollout 기준

### 9.1 guest claim

- 자동 guest → user 병합 금지
- 로그인 후 휴대폰 인증을 통과한 회원만 수동 claim 가능
- 회원 전화번호와 guest/verification 전화번호는 하이픈 유무 차이를 허용한다.
- claim 전에는 preview로 주문/예약/8회권 후보를 확인하고 일부만 선택할 수 있다.
- 8회권 예약을 claim하면 연결된 guest 8회권도 함께 이전한다.
- claim 전에는 기존 guest token 조회를 유지

### 9.2 레거시 경로 유지

- `/orders/detail`, `/bookings/manage` 는 현재 `/guest/**` 로 redirect하며 호환성 경로로 유지한다.
- redirect alias 제거 시점은 별도 배포 단위로 분리한다.

### 9.3 E2E 기준

- guest/admin 기존 smoke 1~5 유지 + member storefront smoke 6~7 추가
- `P8-7`은 회원 8회권 구매, 회원 예약 생성, `/my/bookings/:id` 예약 상세, 변경/취소까지 포함한다
- `P8-8`은 guest 주문·8회권·예약 생성 후 같은 번호의 회원이 `/my`에서 재인증과 선택 claim을 수행하는 흐름을 포함한다

---

## 10. 범위 밖

- 장바구니
- 리뷰/별점/Q&A
- 소셜 로그인
- 네이버페이/스마트스토어 API 연동
- 쿠폰/적립금/찜
- 상품 이미지 CMS

---

## 11. 완료 기준

- 상품 상세가 구매 중심 화면이 된다.
- 회원은 추가 인증 없이 자기 주문/예약/8회권을 조회한다.
- 비회원은 현재 조회 경로를 유지한다.
- 예약/8회권 구매는 탐색 후 제출 직전에만 인증이 뜬다.
- 구현 완료 시 `docs/PRD/0001_spec/spec.md`와 `HANDOFF.md`에 반영된다.
