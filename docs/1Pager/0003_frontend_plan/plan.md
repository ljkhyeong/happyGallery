# Frontend Delivery Plan (Unit-driven)

## 1) 목적
- React + Bootstrap 기반 프론트를 현재 백엔드 계약에 맞춰 단계적으로 구축한다.
- 에이전트가 `단위 1개`씩 안전하게 수행할 수 있도록 프론트 작업과 선행 백엔드 작업을 분리한다.
- 공개 사용자 화면과 관리자 화면을 같은 저장소 안에서 운영하되, 인증/계약/배포 제약을 문서화한 상태로 진행한다.

## 2) 기준 문서
- 스펙: `docs/PRD/0001_spec/spec.md`
- 현재 상태: `HANDOFF.md`
- 리팩토링 계획: `docs/1Pager/0002_refactoring_plan/plan.md`
- 공개 API:
  - `app/src/main/java/com/personal/happygallery/app/web/product/ProductController.java`
  - `app/src/main/java/com/personal/happygallery/app/web/booking/BookingController.java`
  - `app/src/main/java/com/personal/happygallery/app/web/pass/PassController.java`
- 관리자 API:
  - `app/src/main/java/com/personal/happygallery/app/web/admin/*.java`
- 관리자 인증 제약:
  - `app/src/main/java/com/personal/happygallery/app/web/AdminAuthFilter.java`

## 3) 운영 규칙
- 한 번의 지시는 `단위 1개`만 선택한다.
- 프론트 기본 위치는 `frontend/`로 고정한다.
- 스택은 `React + TypeScript + Bootstrap`을 기본으로 하고, 서버 상태 관리는 `TanStack Query`를 우선 검토한다.
- Bootstrap은 레이아웃/폼/모달/테이블의 기반으로 사용하되, 테마 변수와 공통 컴포넌트로 기본 Bootstrap 느낌을 그대로 노출하지 않는다.
- 공개 화면은 현재 백엔드 계약을 넘어서 임의의 API를 가정하지 않는다. 필요한 API가 없으면 먼저 `B*` 단위를 수행한다.
- 관리자 API는 `X-Admin-Key`가 필요하므로 개발용 UI에서만 다루고, 운영 배포용 인증으로 간주하지 않는다.
- API 계약을 바꾸면 구현과 함께 `docs/PRD/0001_spec/spec.md`를 같이 갱신한다.
- 로컬 연동은 우선 CORS 추가보다 `Vite proxy`를 기본으로 한다.

## 4) 지시 템플릿
- `프론트 plan의 F0만 진행해줘. 프론트 초기 뼈대만 만들고, 결과는 변경 파일/핵심 의사결정/실행 검증 형식으로 보고해줘.`
- `프론트 plan의 F4만 진행해줘. 예약 조회/변경/취소 화면만 만들고 백엔드 계약은 바꾸지 마.`
- `프론트 plan의 B2만 진행해줘. 예약 생성에 필요한 공개 조회 API만 추가하고 spec도 같이 갱신해줘.`

## 5) 단위 우선순위
- P0: B1, F0, F1, F2, F3, F4
- P1: B2, B3, F5, F6, F7
- P2: B4, F8, F9

## 5-A) 현재 진행 현황 (2026-03-08)
- 완료:
  - `B1`, `B2`, `B3`, `B4`
  - `F0`, `F1`, `F4`, `F5`, `F6`, `F7`
- 진행 중:
  - `F2`, `F3`, `F8`, `F9`
- 아래 단위 설명과 실행 순서는 최초 계획 보존용이고, 최신 상태 판단은 이 섹션과 `HANDOFF.md`를 우선 기준으로 본다.

---

## B1. 프론트 선행 API 갭 정리
### 범위
- `docs/PRD/0001_spec/spec.md`
- `app/src/main/java/com/personal/happygallery/app/web/**/*.java`
- `docs/1Pager/0003_frontend_plan/plan.md`

### 작업 목표
- 현재 프론트가 바로 붙일 수 있는 API와 부족한 API를 명확히 분리한다.
- 특히 아래 공백을 계약 수준에서 정리한다.
  - 공개 상품 목록 API 부재
  - 공개 슬롯/클래스 조회 API 부재
  - 8회권 구매 시 `guestId`를 브라우저가 얻는 흐름 부재
  - 사용자 주문 생성/조회 API 부재

### 완료 조건
- 프론트 가능한 범위와 백엔드 선행 작업 범위가 문서로 구분되었다.
- 후속 작업 단위들이 어떤 API를 전제로 하는지 설명 가능하다.

### 최소 검증 명령
- 문서 작업만 수행

---

## F0. 프론트 워크스페이스 스캐폴딩
### 범위
- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/tsconfig*.json`
- `frontend/src/**/*`
- 루트 실행 가이드 문서

### 작업 목표
- `Vite + React + TypeScript` 기반 프론트 워크스페이스를 생성한다.
- 기본 라우팅, 절대경로 alias, 환경변수, `Vite proxy(/api -> :8080)`를 설정한다.
- 기본 명령을 고정한다.
  - `npm install`
  - `npm run dev`
  - `npm run build`

### 완료 조건
- `frontend/` 단독으로 개발 서버와 프로덕션 빌드가 가능하다.
- 백엔드 로컬 서버와 프록시 연동할 준비가 끝났다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## F1. 공통 API 클라이언트와 에러 처리 계층
### 범위
- `frontend/src/shared/api/**/*`
- `frontend/src/shared/lib/**/*`
- `frontend/src/shared/types/**/*`

### 작업 목표
- 공통 HTTP 클라이언트, QueryClient, 에러 매핑, 날짜/통화 포맷터를 만든다.
- 백엔드 `ErrorResponse(code, message)`를 기준으로 프론트 에러 처리를 일원화한다.
- 예약/패스/상품/Admin DTO 타입의 초안 구조를 만든다.

### 완료 조건
- 화면 구현 전에 재사용 가능한 API 호출 규약이 생겼다.
- `PHONE_VERIFICATION_FAILED`, `BOOKING_CONFLICT`, `UNAUTHORIZED` 같은 주요 에러를 UI에서 분기할 수 있다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## F2. 앱 셸, 테마, 공통 UI 기반
### 범위
- `frontend/src/app/**/*`
- `frontend/src/shared/ui/**/*`
- `frontend/src/styles/**/*`

### 작업 목표
- 공통 레이아웃, 라우터, 로딩/에러 상태, 토스트, 404 페이지를 만든다.
- Bootstrap 기본값을 그대로 쓰지 않고 공방 서비스에 맞는 컬러/타이포/간격 토큰을 정의한다.
- 모바일과 데스크톱 모두에서 무너지지 않는 반응형 셸을 만든다.

### 완료 조건
- 이후 기능 화면이 공통 레이아웃 위에 얹힐 수 있다.
- 공통 상태 표시(UI skeleton, empty, error)가 재사용 가능하다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## F3. 관리자 상품/슬롯 화면 MVP
### 범위
- `frontend/src/features/admin-product/**/*`
- `frontend/src/features/admin-slot/**/*`
- `frontend/src/pages/admin/**/*`

### 작업 목표
- 현재 이미 존재하는 관리자 API를 붙여 다음 화면을 구현한다.
  - 상품 목록 조회
  - 상품 등록
  - 슬롯 생성
  - 슬롯 비활성화
- 개발용 `X-Admin-Key` 입력/보관 방식을 최소 범위에서 정한다.

### 완료 조건
- 관리자가 상품과 슬롯을 브라우저에서 직접 운영할 수 있다.
- 인증 실패(`401 UNAUTHORIZED`)와 검증 오류(`400 INVALID_INPUT`)가 화면에서 구분된다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## F4. 예약 조회/변경/취소 화면
### 범위
- `frontend/src/features/booking-manage/**/*`
- `frontend/src/pages/BookingManagePage.tsx`

### 작업 목표
- 현재 공개 API만 사용해 비회원 예약 관리 화면을 구현한다.
  - 예약 조회 (`bookingId + token`)
  - 예약 변경
  - 예약 취소
- 예약번호/토큰 입력 UX, 상태 배지, 환불 가능 여부 표시를 정리한다.

### 완료 조건
- 예약 생성 이후 사용자가 조회/변경/취소 플로우를 웹에서 처리할 수 있다.
- `CHANGE_NOT_ALLOWED`, `REFUND_NOT_ALLOWED`, `BOOKING_CONFLICT`를 명확히 표시한다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## B2. 예약 생성용 공개 조회 API 추가
### 범위
- `app/src/main/java/com/personal/happygallery/app/web/**/*`
- `app/src/main/java/com/personal/happygallery/app/booking/**/*`
- `infra/src/main/java/com/personal/happygallery/infra/booking/**/*`
- `docs/PRD/0001_spec/spec.md`

### 작업 목표
- 예약 생성 화면에 필요한 공개 조회 API를 추가한다.
- 최소 후보:
  - 클래스 목록 조회
  - 날짜/클래스 기준 슬롯 목록 조회
  - 슬롯별 잔여 정원/활성 여부 조회
- 프론트가 관리자 API를 우회 호출하지 않도록 공개 계약을 분리한다.

### 완료 조건
- 사용자가 예약 전에 “어떤 클래스/시간을 선택할 수 있는지” 브라우저에서 조회 가능하다.
- API 계약과 스펙 문서가 일치한다.

### 최소 검증 명령
- `./gradlew :app:policyTest`
- `./gradlew --no-daemon :app:useCaseTest`

---

## F5. 공개 상품 카탈로그 화면
### 범위
- `frontend/src/features/product/**/*`
- `frontend/src/pages/ProductListPage.tsx`
- `frontend/src/pages/ProductDetailPage.tsx`

### 작업 목표
- 상품 목록/상세 화면을 만든다.
- 현재 상세 API만 있으면 상세 먼저 구현하고, 목록 API가 추가되면 목록까지 확장한다.
- 상품 유형, 가격, 구매 가능 여부를 명확히 보여준다.

### 완료 조건
- 사용자가 상품 상세를 확인할 수 있다.
- 목록 API가 있으면 카탈로그 진입점도 함께 제공된다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## B3. 8회권 구매 계약 보완
### 범위
- `app/src/main/java/com/personal/happygallery/app/web/pass/**/*`
- `app/src/main/java/com/personal/happygallery/app/pass/**/*`
- `infra/src/main/java/com/personal/happygallery/infra/booking/**/*`
- `docs/PRD/0001_spec/spec.md`

### 작업 목표
- 브라우저가 직접 알 수 없는 `guestId` 의존을 제거하거나 대체 흐름을 만든다.
- 예시 방향:
  - `guestId` 대신 전화번호/이름 기반 구매
  - 게스트 조회/생성 API 추가
  - 예약 생성/인증 흐름과 연계되는 식별자 발급

### 완료 조건
- 사용자가 내부 DB ID를 몰라도 8회권 구매가 가능하다.
- 공개 API 계약이 프론트에서 사용할 수 있는 형태가 되었다.

### 최소 검증 명령
- `./gradlew :app:policyTest`
- `./gradlew --no-daemon :app:useCaseTest`

---

## F6. 예약 생성 화면
### 범위
- `frontend/src/features/booking-create/**/*`
- `frontend/src/pages/BookingCreatePage.tsx`

### 작업 목표
- 휴대폰 인증 코드 발송, 코드 입력, 슬롯 선택, 예약 생성까지 한 화면 흐름으로 구현한다.
- 예약금 결제 경로와 8회권 사용 경로를 분기한다.
- 성공 시 예약번호와 access token을 즉시 보여준다.

### 완료 조건
- 사용자가 프론트에서 비회원 예약을 끝까지 완료할 수 있다.
- 실패 케이스(`PHONE_VERIFICATION_FAILED`, `CAPACITY_EXCEEDED`, `SLOT_NOT_AVAILABLE`)가 정확히 보인다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## F7. 8회권 구매 화면
### 범위
- `frontend/src/features/pass/**/*`
- `frontend/src/pages/PassPurchasePage.tsx`

### 작업 목표
- 8회권 구매 UI를 구현한다.
- 가격, 만료일 규칙, 잔여 횟수 의미를 화면에서 설명한다.
- 구매 후 예약 생성 흐름과 연결 가능한 진입점을 만든다.

### 완료 조건
- 사용자가 공개 화면에서 8회권을 구매할 수 있다.
- 백엔드 계약에 맞는 입력값만 전송한다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## B4. 사용자 주문 API 계약 추가
### 범위
- `app/src/main/java/com/personal/happygallery/app/order/**/*`
- `app/src/main/java/com/personal/happygallery/app/web/**/*`
- `infra/src/main/java/com/personal/happygallery/infra/order/**/*`
- `docs/PRD/0001_spec/spec.md`

### 작업 목표
- 사용자용 주문 생성/조회 API를 정의하고 구현한다.
- 최소 후보:
  - 상품 주문 생성
  - 주문 상세 조회
  - 배송/픽업/제작 상태 조회
- 관리자 API와 사용자 API를 섞지 않는다.

### 완료 조건
- 사용자 프론트에서 주문을 생성하고 상태를 확인할 최소 계약이 준비되었다.
- 스펙과 실제 응답이 일치한다.

### 최소 검증 명령
- `./gradlew :app:policyTest`
- `./gradlew --no-daemon :app:useCaseTest`

---

## F8. 관리자 운영 화면 확장
### 범위
- `frontend/src/features/admin-order/**/*`
- `frontend/src/features/admin-refund/**/*`
- `frontend/src/features/admin-pass/**/*`
- `frontend/src/pages/admin/**/*`

### 작업 목표
- 현재 존재하는 관리자 운영 API를 붙여 추가 화면을 만든다.
  - 환불 실패 목록/재시도
  - 패스 만료 배치 수동 트리거
  - 패스 전체 환불
  - 주문 승인/거절/제작완료/픽업준비/픽업완료/픽업만료 배치
- 상태 전이 버튼은 낙관적 업데이트보다 서버 응답 재조회 기준으로 구현한다.

### 완료 조건
- 운영자가 주요 배치/후속처리를 관리자 화면에서 실행할 수 있다.
- 충돌/실패 응답이 묻히지 않고 그대로 드러난다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## F9. 주문 화면과 최종 연결
### 범위
- `frontend/src/features/order/**/*`
- `frontend/src/pages/OrderCreatePage.tsx`
- `frontend/src/pages/OrderDetailPage.tsx`

### 작업 목표
- B4에서 추가된 사용자 주문 계약을 기준으로 상품 주문 화면을 구현한다.
- 배송/픽업/제작 상태 표시를 시각적으로 구분한다.
- 예약/상품/패스 진입점을 하나의 사용자 경험으로 연결한다.

### 완료 조건
- 사용자 관점의 핵심 공개 기능(상품, 예약, 패스, 주문)이 프론트에서 이어진다.
- 현재 백엔드 계약을 벗어나는 임시 mock 없이 동작한다.

### 최소 검증 명령
- `cd frontend && npm run build`

---

## 6) 권장 실행 순서
1. `B1 -> F0 -> F1 -> F2`
2. `F3 -> F4`
3. `B2 -> F5 -> F6`
4. `B3 -> F7`
5. `B4 -> F8 -> F9`

## 7) 병렬화 가이드
- 동시에 진행 가능: `F3`, `F4`
- 동시에 진행 가능: `B2`, `F5` (단, F5는 상세 화면 우선)
- 순차 권장: `B2 -> F6`, `B3 -> F7`, `B4 -> F9`
- 충돌 위험 높음: `F1`, `F2`는 다른 모든 프론트 단위보다 먼저 끝내는 편이 낫다.

## 8) 현재 기준 메모
- 지금 바로 구현 가능한 프론트 단위:
  - 신규 백엔드 보강 없이 `F2`, `F3`, `F8`, `F9`를 계속 진행할 수 있다.
- 이미 완료된 단위:
  - `B1`, `B2`, `B3`, `B4`
  - `F0`, `F1`, `F4`, `F5`, `F6`, `F7`
- 현재 계약 기준으로 이미 가능한 API:
  - 공개 상품 목록/상세
  - 공개 클래스/슬롯 조회
  - 공개 예약 생성/조회/변경/취소
  - 공개 8회권 구매
  - 공개 주문 생성/조회
  - 관리자 상품/슬롯/주문/환불 운영 API
- 관리자 화면은 현재 `X-Admin-Key` 헤더 기반이므로, 개발/내부 운영용으로 한정해 시작한다.
