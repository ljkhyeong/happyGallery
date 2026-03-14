# P8 E2E Checklist

`P8`은 핵심 사용자/운영자 흐름을 실제 로컬 실행 환경에서 끝까지 검증하기 위한 체크리스트다.  
현재 저장소에는 Playwright smoke와 보완용 수동 점검 기준이 함께 들어 있다.

---

## 목적

- 사용자 플로우와 관리자 운영 플로우를 실제 브라우저 기준으로 검증한다.
- 발견된 버그는 별도 단위로 분리하거나 같은 작업 안에서 즉시 수정한다.
- 완전 자동화가 아직 어려운 구간은 수동 체크리스트로 명시해 누락 없이 점검한다.

## 실행 전 준비

1. MySQL 실행

```bash
docker compose up -d mysql
```

2. 백엔드 실행

```bash
./gradlew :app:bootRun
```

3. Playwright 브라우저 설치 1회

```bash
cd frontend
npm run e2e:install
```

4. Smoke 실행

```bash
cd frontend
npm run e2e
```

참고:
- Playwright는 기본적으로 Vite dev server를 직접 띄우거나 이미 떠 있는 `localhost:3000`을 재사용한다.
- 백엔드는 기본 `http://localhost:8080`에 떠 있어야 한다.
- 관리자 로그인 기본값은 `admin` / `admin1234`다. 필요하면 `PLAYWRIGHT_ADMIN_USERNAME`, `PLAYWRIGHT_ADMIN_PASSWORD`로 덮어쓴다.
- 관리자 보조 API 호출은 `/api/v1/admin/auth/login`으로 얻은 Bearer 토큰을 사용한다.
- 다른 백엔드 주소를 쓰려면 `PLAYWRIGHT_BACKEND_URL`로 덮어쓴다.
- 앱 컨테이너가 8080을 쓰고 있으면 로컬 `bootRun` 전에 `docker compose stop app`을 먼저 실행한다.
- `local` 프로필 `bootRun`은 `classes` 테이블이 비어 있을 때 기본 클래스 3종을 자동 seed한다.
- 시나리오 5는 local 전용 dev hook `POST /api/v1/admin/dev/payment/refunds/fail-next`와 `DELETE /api/v1/admin/dev/payment/refunds/fail-next`를 사용한다.
  요청 바디에 `orderId`를 넣으면 특정 주문의 다음 환불 1회만 실패시킬 수 있다.

## 최신 실행 결과

- 실제 로컬 smoke 실행 기준:
  - pass: 시나리오 1, 2, 3, 4, 5
- 검증 메모:
  - clean DB에서 `:app:bootRun` 시 기본 클래스 seed와 관리자 기본 계정 정합화가 반영된 뒤 smoke 1~5를 통과했다.
  - 슬롯 생성 race condition과 브라우저/Node 시간 포맷 차이, `8회권 사용` 라디오 접근성 연결을 보강했다.
  - 시나리오 5는 주문 거절 직전에 환불 실패를 1회 arm한 뒤, 관리자 환불 실패 목록에서 재시도까지 검증한다.

## 현재 자동화 범위

### 자동화 완료

1. 상품 등록 -> 관리자 목록 확인
2. 슬롯 생성 -> 예약 생성 -> 예약 조회/변경/취소
3. 8회권 구매 -> 8회권으로 예약
4. 주문 생성 -> 관리자 승인 -> 픽업 준비 -> 픽업 완료
5. 환불 실패 -> 재시도

## 수동 체크리스트

### 시나리오 1. 상품 등록 -> 목록 확인

- `/admin`에서 관리자 인증
- 상품 등록 카드에서 상품 생성
- 상품 목록 카드에서 방금 등록한 상품명, 유형, 상태 확인

### 시나리오 2. 클래스/슬롯 생성 -> 예약 생성 -> 예약 조회/변경/취소

- `/admin`에서 같은 클래스에 미래 슬롯 2개 생성
- `/bookings/new`에서 휴대폰 인증 후 예약 생성
- 성공 카드에서 booking token 확인
- `/bookings/manage`에서 예약 조회
- 새 슬롯 ID로 예약 변경
- 예약 취소 후 상태가 `취소됨`으로 바뀌는지 확인

### 시나리오 3. 8회권 구매 -> 8회권으로 예약

- `/passes/purchase`에서 8회권 구매
- 성공 카드에서 pass ID 확인
- `/bookings/new`에서 `8회권 사용` 선택 후 예약 생성
- 관리자 예약 목록 또는 예약 성공 카드 기준으로 pass booking 여부 확인

### 시나리오 4. 주문 생성 -> 관리자 승인 -> 픽업 준비 -> 픽업 완료

- `/orders/new`에서 주문 생성
- 성공 카드에서 order ID / token 확인
- `/admin` 주문 목록에서 승인
- `이행 대기`에서 픽업 준비
- `픽업 대기`에서 픽업 완료
- `/orders/detail`에서 최종 상태가 `수령 완료`인지 확인

### 시나리오 5. 환불 실패 -> 재시도

- `/orders/new`에서 주문을 생성한다.
- local dev hook으로 다음 환불 1회를 실패하도록 arm한다.
- `/admin` 주문 목록에서 해당 주문을 거절해 FAILED refund를 만든다.
- 환불 실패 목록에서 생성된 row를 확인하고 `재시도`를 클릭한다.
- 동일 refund가 실패 목록에서 사라지는지 확인한다.

## 후속 과제

1. 안정적인 `data-testid` 추가로 Playwright selector 취약성 완화
2. 실 PG 연동 전환 시 local dev hook을 테스트 전용 경계로 재정리
