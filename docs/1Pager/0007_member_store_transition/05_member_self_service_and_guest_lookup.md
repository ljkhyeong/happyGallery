# U5. Member Self-service And Guest Lookup

이 단위는 회원용 "내 정보" 흐름과 비회원 조회 경로를 분리하는 작업이다.

성격:
- 백엔드/프론트 혼합
- 고객 프론트의 실제 운영성을 결정하는 단위

---

## 1. 목표

- 회원은 `내 주문`, `내 예약`, `내 8회권`에서 자기 이력을 추가 인증 없이 본다.
- 비회원은 기존 조회/변경 경로를 계속 사용할 수 있다.
- 현재 `예약 조회`, `주문 조회`가 사실상 비회원 전용이라는 혼선을 없앤다.

---

## 2. 범위

포함:
- 회원 마이페이지 라우트
- 회원 전용 목록/상세 API 연동
- guest claim preview / verify / claim API
- 마이페이지 비회원 이력 가져오기 모달
- 비회원 조회 화면 라벨/경로 재정리
- 회원 예약 변경/취소, 주문 상태 조회, 8회권 조회 UX

제외:
- 리뷰/주문 후 추천

---

## 3. 권장 라우트

- 회원:
  - `/my`
  - `/my/orders`
  - `/my/orders/:id`
  - `/my/bookings`
  - `/my/bookings/:id`
  - `/my/passes`
- 비회원:
  - `/guest/orders`
  - `/guest/bookings`

회원 API는 `/api/v1/me/**`를 유지하고, 비회원 조회는 `/guest/orders`, `/guest/bookings`로만 노출한다.

---

## 4. 주요 작업

백엔드:
- `me` 계열 조회 API
- 회원 소유권 검증
- guest claim 수동 이전 API와 휴대폰 재인증
- 회원 예약 변경/취소 권한 처리

프론트:
- 마이페이지 셸
- 로그인 게이트, 요약 통계 카드, 다음 예약/guest claim 진입을 포함한 `/my` 대시보드 정리
- 주문/예약/8회권 목록 및 상세 (`/my/orders`, `/my/bookings`, `/my/passes`, 검색/상태 필터/quick tab/정렬, 상세 페이지)
- 회원 예약 상세 화면에서 변경/취소 액션 제공
- `비회원 이력 가져오기` 모달과 선택 claim UX
- `/my?claim=1` 자동 모달 오픈 뒤에도 후속 claim 안내를 남기는 대시보드 UX
- 기존 guest 조회 페이지의 카피와 경로 정리, `회원 내 정보`/`비회원 조회` 진입 버튼 정리
- Layout 상단에 `내 정보` 진입점 추가

---

## 5. 완료 기준

- 회원은 토큰/휴대폰 인증 입력 없이 자신의 데이터에 접근 가능
- 회원은 `/my` 첫 화면에서 최근 주문/예약/8회권 요약과 다음 액션을 바로 이해할 수 있음
- 회원은 `/my/orders`, `/my/bookings`, `/my/passes`에서 전체 이력을 분리해서 볼 수 있음
- 회원은 `/my` 목록 페이지에서 검색, 상태 필터, quick tab, 정렬로 원하는 이력을 바로 좁힐 수 있음
- 회원은 같은 휴대폰 번호의 guest 이력을 재인증 후 수동 claim 할 수 있음
- 회원은 guest 성공 화면이나 member gate에서 로그인/회원가입 전환을 오가도 `redirect`/`claim` 문맥을 잃지 않음
- 비회원은 여전히 기존 방식으로 조회 가능
- 회원 예약 상세 화면에서 변경/취소가 가능함
- 사용자가 현재 화면이 회원용인지 비회원용인지 혼동하지 않는다

---

## 6. 최소 검증

- `./gradlew --no-daemon :app:test --tests ...Order... --tests ...Booking... --tests ...Pass...`
- `cd frontend && npm run build`
- `cd frontend && npm run e2e -- --grep "P8-7"`

---

## 7. 다음 단위로 넘길 산출물

- 회원용 라우트 맵
- guest 라우트 대체 정책
- guest claim 수동 이전 정책
- `401`, `403`, `404` 화면 처리 기준
