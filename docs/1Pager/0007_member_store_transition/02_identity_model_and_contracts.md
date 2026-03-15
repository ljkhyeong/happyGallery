# U2. Identity Model And Contracts

이 단위는 주문, 예약, 8회권을 "회원/비회원 공존" 모델로 다시 정의하는 작업이다.

성격:
- 백엔드 중심
- API 계약과 데이터 모델 정리가 핵심

---

## 1. 목표

- 주문/예약/8회권 생성과 조회가 `user_id` 또는 `guest_id`를 명시적으로 타게 한다.
- 회원 조회 API와 비회원 조회 API를 분리한다.
- 기존 guest API를 유지하면서 회원 전용 API를 추가한다.

---

## 2. 범위

포함:
- 주문 생성/조회 계약 재정의
- 예약 생성/조회/변경/취소 계약 재정의
- 8회권 구매/조회 계약 재정의
- 회원 전용 `me` 계열 API
- 비회원 조회 유지 전략

제외:
- 상품 상세 구매 UI
- 예약/8회권 제출 직전 인증 UX

---

## 3. 핵심 정책

- 회원:
  - 생성 시 전화 인증을 매번 요구하지 않는다
  - 조회는 `내 주문`, `내 예약`, `내 8회권` API로 처리
- 비회원:
  - 생성은 guest checkout + 휴대폰 인증 유지
  - 조회는 기존 token/휴대폰 인증 경로 유지
- 서버는 생성 요청에서 현재 인증 사용자를 우선 해석하고, 없을 때만 guest payload를 받는다.

---

## 4. 주요 작업

주문:
- 회원 주문 생성 API 추가 또는 기존 주문 생성 API의 인증 분기 추가
- `GET /api/v1/me/orders`
- `GET /api/v1/me/orders/{id}`
- 기존 `GET /api/v1/orders/{orderId}?token=...` 유지

예약:
- 회원 예약 생성 경로 추가
- `GET /api/v1/me/bookings`
- `GET /api/v1/me/bookings/{id}`
- 기존 비회원 `bookingId + token` 조회 유지

8회권:
- 회원 구매 API 추가
- `GET /api/v1/me/passes`
- `GET /api/v1/me/passes/{id}`
- guest 구매/조회 계약은 최소 범위 유지

공통:
- 서비스 계층에서 `user_id`/`guest_id` 동시 설정 금지 가드
- 조회 권한 검증 공통화

---

## 5. 권장 API 방향

- 회원 전용:
  - `/api/v1/me/orders`
  - `/api/v1/me/bookings`
  - `/api/v1/me/passes`
- guest 전용:
  - 현재 `/orders/{id}?token=...`
  - 현재 `/bookings/{id}?token=...`
  - 필요 시 `/guest/...` 접두사로 재정리

---

## 6. 완료 기준

- 회원은 토큰/휴대폰 인증 없이 자기 이력 조회 가능
- 비회원은 기존 조회 경로가 깨지지 않음
- `orders`, `bookings`, `pass_purchases`의 식별 규칙이 문서와 코드에 일치

---

## 7. 최소 검증

- `./gradlew --no-daemon :app:test --tests ...Order... --tests ...Booking... --tests ...Pass...`
- 회원/비회원 happy-path, unauthorized-path 통합 테스트

---

## 8. 다음 단위로 넘길 산출물

- 회원/비회원별 생성 요청 예시
- 조회 API 계약 초안
- `me` 계열 DTO
- guest API 유지 범위 목록
