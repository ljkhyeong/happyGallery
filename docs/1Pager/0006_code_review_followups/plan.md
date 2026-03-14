# Code Review Follow-ups

P8, P9, P10 완료 이후 운영 리스크와 UX 결함, 인증/테스트 경계, 구조 리팩토링 후보를 정리한 후속 플랜이다.

---

## 한눈에 보기

- 기준: `HANDOFF.md`, `docs/PRD/0001_spec/spec.md`
- 목적: 배포 전 리스크를 줄이고, 다음 리팩토링/기능 추가 우선순위를 명확히 한다.
- 우선 처리: 인증/캐시/조회 stale state
- 다음 처리: 운영 보호 장치와 테스트 정밀도
- 장기 과제: 주문 이행 모델 단순화, 감사 로그 보강, 운영 화면 확장

---

## P1. 인증 및 조회 상태 정합성

### 목표

- 로그아웃 또는 관리자 세션 전환 시 이전 관리자 데이터가 재사용되지 않게 한다.
- 예약/주문 조회 실패 시 이전 성공 결과가 화면에 남지 않게 한다.
- 예약 생성에서 클래스/날짜 변경 후 stale slot 제출이 불가능하게 한다.

### 작업 항목

1. 관리자 로그아웃 시 React Query admin 캐시를 비운다.
2. admin query key에 토큰 또는 session epoch를 반영한다.
3. 예약 조회/주문 조회 실패 시 현재 상세 state를 명시적으로 초기화한다.
4. 예약 생성에서 클래스/날짜 변경 시 선택 슬롯을 해제한다.

### 완료 조건

- 로그아웃 직후 재로그인해도 이전 관리자 데이터가 잠깐이라도 남지 않는다.
- 잘못된 토큰으로 예약/주문 재조회하면 이전 상세/액션 UI가 사라진다.
- 슬롯 필터 변경 후 이전 슬롯 ID로 제출되지 않는다.

### 최소 검증

- `cd frontend && npm run build`
- 관련 UI 단위 검증 또는 Playwright unhappy-path 시나리오

---

## P2. 운영 인증 및 테스트 경계 보강

### 목표

- admin E2E가 실제 Bearer 인증 경로를 검증하게 한다.
- 로그인/관리자 경로에 대한 기본 보호 장치를 보강한다.

### 작업 항목

1. Playwright support의 admin helper를 `X-Admin-Key` fallback이 아니라 실제 로그인 세션 기반 조회로 전환한다.
2. `/api/v1/admin/auth/login`에 별도 rate limit을 추가한다.
3. Bearer 세션에서 검증된 admin id를 요청 컨텍스트에 싣고, `X-Admin-Id` 헤더 위조 없이 운영 이력에 기록되게 한다.
4. admin login / session expiry / bearer-only 환경에 대한 smoke 또는 integration test를 추가한다.

### 완료 조건

- `X-Admin-Key`를 꺼도 admin smoke가 통과한다.
- 로그인 brute force 기본 방어가 있다.
- 주문 승인/거절/환불 재시도 이력의 admin 식별자가 세션과 일치한다.

### 최소 검증

- `./gradlew --no-daemon :app:test --tests ...AdminAuth... --tests ...RateLimit...`
- `cd frontend && npm run e2e`

---

## P3. 운영 메모리/인프라 리스크 완화

### 목표

- 인메모리 세션/버킷 저장소의 무제한 증가를 줄인다.
- 프록시 환경에서 rate limit 키를 더 안전하게 계산한다.

### 작업 항목

1. `AdminSessionStore`에 만료 세션 정리 전략을 추가한다.
2. `RateLimitFilter` bucket map에 eviction 또는 bounded cleanup을 넣는다.
3. `X-Forwarded-For` 신뢰 정책을 프록시 환경 기준으로 제한한다.

### 완료 조건

- 장시간 실행 후에도 세션/버킷 저장소가 무기한 커지지 않는다.
- 임의 헤더 변경만으로 rate limit 우회가 어렵다.

### 최소 검증

- `./gradlew --no-daemon :app:test --tests ...RateLimit...`
- 메모리/정책 단위 테스트

---

## P4. UX 및 운영 화면 후속

### 목표

- 운영 화면의 기본값과 로딩 실패 UX를 현실적인 수준으로 보강한다.
- 운영자가 이력과 상태 변화를 추적하기 쉽게 만든다.

### 작업 항목

1. 관리자 예약 목록 기본 날짜를 Asia/Seoul 기준 오늘로 맞춘다.
2. 주문 상품/예약 슬롯 로딩 실패 시 명시적 에러 UI를 추가한다.
3. 승인/거절/환불 재시도/노쇼 처리 이력을 한 화면에서 볼 수 있는 운영 이력 화면을 검토한다.
4. 환불 실패/재시도 이력을 별도 로그 또는 history 테이블로 남기는 방향을 설계한다.

### 완료 조건

- 한국 시간 오전에도 관리자 예약 목록 기본 날짜가 오늘이다.
- 주요 로딩 실패가 빈 화면처럼 보이지 않는다.

---

## P5. 장기 리팩토링 및 기능 공백

### 목표

- 남아 있는 구조 복잡도를 줄이고, 운영상 비어 있는 흐름을 메운다.

### 작업 항목

1. `DELAY_REQUESTED` 이후 재개/확정 흐름을 설계하고 구현한다.
2. 주문당 fulfillment 단일성을 보장하고, 제작 주문에서 `completeProduction -> prepare-pickup` 시 fulfillment 중복 생성이 없게 한다.
3. `Order.status` 와 실제 환불 성공/실패 표현을 분리해 `*_REFUNDED` 상태 오해를 줄인다.
4. `Order.status` 와 `Fulfillment.status` 이중 관리 구조를 단순화한다.
5. PG 환불 패턴을 `RefundExecutor` 같은 공통 경계로 모으는 방향을 검토한다.
6. local refund failure hook을 refundId/orderId 범위 기반으로 좁혀 오동작 가능성을 줄인다.

### 완료 조건

- 지연 요청 이후 운영자가 다음 액션을 명확히 실행할 수 있다.
- 주문당 fulfillment가 1건이라는 불변식이 깨지지 않는다.
- 환불 실패 주문이 상태명만 보면 이미 환불 완료처럼 보이지 않는다.
- 주문/이행 상태 불일치 가능성이 줄어든다.

---

## 권장 순서

1. `P1`
2. `P2`
3. `P3`
4. `P4`
5. `P5`
