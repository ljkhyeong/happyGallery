# U6. Rollout, Migration And E2E

이 단위는 구현 막바지에 필요한 데이터 이관, 테스트 매트릭스, 운영 rollout 기준을 정리하는 작업이다.

성격:
- 통합/운영
- 다른 단위 완료 후 마무리

---

## 1. 목표

- 회원/비회원 공존 모델이 운영 중에 깨지지 않도록 데이터와 테스트 기준을 정리한다.
- guest 이력과 신규 회원 이력의 연결 규칙을 명문화한다.

---

## 2. 범위

포함:
- guest claim 정책
- migration/seed 정리
- E2E 시나리오 확장
- rollout 체크리스트

제외:
- 신규 기능 자체 구현

---

## 3. 선결정 정책

- 자동 병합 금지:
  - 같은 전화번호만으로 guest 이력을 user에 자동 귀속하지 않는다.
- 권장 연결 방식:
  - 로그인한 회원이 휴대폰 인증을 통과한 뒤 guest 이력을 선택적으로 claim
- 레거시 guest 링크/토큰은 기존 TTL 정책을 유지한다.

## 4. 현재 구현 스냅샷

회원 UI:
- `/login`
- `/signup`
- `/my`
- `/my/orders`
- `/my/orders/:id`
- `/my/bookings`
- `/my/bookings/:id`
- `/my/passes`

회원 API:
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me`
- `GET /api/v1/me/orders`
- `POST /api/v1/me/orders`
- `GET /api/v1/me/bookings`
- `POST /api/v1/me/bookings`
- `PATCH /api/v1/me/bookings/{id}/reschedule`
- `DELETE /api/v1/me/bookings/{id}`
- `GET /api/v1/me/passes`
- `POST /api/v1/me/passes`
- `GET /api/v1/me/guest-claims/preview`
- `POST /api/v1/me/guest-claims/verify`
- `POST /api/v1/me/guest-claims`

레거시 guest UI:
- `/orders/new`
- `/guest/orders`
- `/guest/bookings`
- `/passes/purchase`
- `/bookings/new`

운영 메모:
- `/bookings/new`, `/passes/purchase`, `/products/:id` 는 제출 직전 인증 게이트를 사용한다.
- `/orders/new` 는 legacy guest 주문 생성 fallback을 유지하고, guest 조회는 `/guest/**` canonical route만 사용한다.
- 회원 예약 변경/취소와 guest claim UI는 `/my` 기준으로 구현 완료 상태다.
- guest 성공 화면의 회원가입/로그인 CTA는 `/my?claim=1` 로 이어지고, `/my`는 claim 모달을 자동으로 연다.

---

## 5. Route / API Migration 표

| 영역 | 현재 유지 경로 | 목표 상태 | U6 판단 |
|------|----------------|-----------|---------|
| 회원 진입 | `/login`, `/signup`, `/my` | 유지 | 유지 |
| 회원 주문 조회 | `/my`, `/my/orders`, `/my/orders/:id` | 유지 | 유지 |
| 회원 예약/8회권 조회 | `/my`, `/my/bookings`, `/my/bookings/:id`, `/my/passes` | 유지 | 유지 |
| guest 주문 조회 | `/guest/orders` | 유지 | canonical route만 유지 |
| guest 예약 조회 | `/guest/bookings` | 유지 | canonical route만 유지 |
| guest 주문 생성 | `/orders/new` | 상품 상세 fallback 또는 `/guest/orders/new` | 유지 |
| guest 예약/8회권 생성 | `/bookings/new`, `/passes/purchase` | 유지 | 인증 게이트 적용 완료 |
| 회원 API | `/api/v1/me/**` | 유지 | 기준 API로 사용 |
| guest 조회 API | token 기반 `/api/v1/orders/*`, `/api/v1/bookings/*` | 유지 | TTL 정책 유지 |

---

## 6. 테스트 매트릭스

- 회원 주문 생성 → 내 주문 조회
- 비회원 주문 생성 → guest 주문 조회
- 회원 예약 생성 → 내 예약 조회
- 비회원 예약 생성 → guest 예약 변경/취소
- 회원 8회권 구매 → 내 8회권 조회
- guest 8회권 구매 → 기존 guest 경로 유지
- 로그아웃 후 member route 접근 차단
- guest token 없이 비회원 조회 차단

---

## 7. E2E 자동화 범위

자동화 완료:
1. 상품 등록 → 관리자 목록 확인
2. guest 예약 생성 → guest 예약 조회/변경/취소
3. guest 8회권 구매 → guest 8회권 예약
4. guest 주문 생성 → 관리자 승인 → 픽업 완료
5. 환불 실패 → 관리자 재시도
6. 회원 가입 → 상품 상세 주문 → 내 주문 상세
7. 회원 가입 → 8회권 구매 → 회원 예약 생성 → 내 정보 확인
8. guest 주문·8회권·예약 생성 → 같은 번호의 회원이 `/my`에서 claim
9. guest 주문 성공 → 회원가입 → `/my` claim 모달 자동 진입

문서만 있고 아직 브라우저 자동화가 없는 항목:
- 없음

---

## 8. guest claim 정책

불변 원칙:
- 전화번호 일치만으로 guest 이력을 user에 자동 귀속하지 않는다.
- claim은 로그인된 회원이 명시적으로 시작해야 한다.
- claim 전에는 기존 guest token 접근이 계속 유효하다.

권장 절차:
1. 회원 로그인
2. 휴대폰 인증으로 본인 전화번호 재검증
3. 전화번호가 일치하는 guest 주문/예약/8회권 목록을 제시
4. 사용자가 선택한 항목만 `user_id`로 귀속
5. claim 이력은 감사 로그로 남김

현재 구현:
- `/api/v1/me/guest-claims/{preview,verify,claim}`
- `/my` 의 `비회원 이력 가져오기` 모달
- Playwright `P8-8` guest claim smoke
- Playwright `P8-9` guest 성공 화면 → 회원가입 → claim 모달 자동 진입 smoke
- 로그인/회원가입 페이지의 `redirect`/`claim`/회원가입 prefill(`name`/`phone`) 컨텍스트 유지

---

## 9. 주요 작업

- seed 데이터와 기본 계정 정리
- E2E helper를 회원/비회원 분기로 확장
- 문서상 route/API migration 표 작성
- 운영 배포 체크리스트 작성

---

## 10. Rollout 체크리스트

1. local/dev에서 회원/비회원 smoke 1~9를 먼저 통과시킨다.
2. 운영 문서에 `/guest/**` canonical route 유지 범위를 명시한다.
3. `/api/v1/me/**` 는 HttpOnly 세션 기준으로만 열고, guest token 조회와 혼용하지 않는다.
4. guest claim API가 있어도 “회원가입해도 기존 비회원 이력은 자동 연결되지 않음”을 운영 공지에 포함한다.
5. member route 장애 시 guest 조회는 `/guest/orders`, `/guest/bookings` canonical route로 안내한다.
6. `/guest/**` route 변경은 member route 안정화 후 별도 배포 단위로 분리한다.

---

## 11. 완료 기준

- 회원/비회원 공존 시나리오가 테스트 문서와 E2E에서 모두 커버된다.
- rollout 시 어떤 레거시 경로를 유지/제거하는지 명확하다.

---

## 12. 최소 검증

- `./gradlew --no-daemon :app:test --tests ...`
- `cd frontend && npm run build`
- `cd frontend && npm run e2e`

---

## 13. 최종 산출물

- 운영 배포 체크리스트
- guest claim 정책 문서
- 회원/비회원 E2E 시나리오 목록
- 레거시 경로 제거 로드맵
