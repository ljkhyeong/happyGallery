# HANDOFF

이 파일은 다음 **AI 에이전트용 인수인계 문서**다.  
사람용 긴 변경 이력은 두지 않는다. 현재 상태, 우선순위, 작업 규칙만 짧게 유지한다.

## 🚧 진행 중: 돈·신원 경로 복원 플랜 (Phase 1 백엔드 전환 구현됨)

**갱신 시점**: 2026-04-22
**브랜치**: `payment-integration` (이미 분기됨)
**플랜 전문**: `~/.claude/plans/imperative-greeting-barto.md` — **반드시 먼저 읽는다.** 경로는 세션마다 고정.

### 배경

`plan.md`의 Track 1~5는 전부 완료. 2026-04-19 세션에서 운영 애로 전수 점검 후 3-Phase 플랜으로 확정. 현재 남은 구조적 애로:

1. **결제 없이 구매 성공** — Order/Booking/PassPurchase가 PG 호출 없이 바로 저장. `FakePaymentProvider`가 `@Primary`/`@Profile` 없이 상시 동작.
2. **SMS 인증 미발송** — `DefaultGuestBookingService.sendVerificationCode`가 로그만 남김.
3. **회원 가입 휴대폰 소유 확인 없음** — `DefaultCustomerAuthService.signup`이 전화번호 문자열 그대로 저장.

### 선택된 접근

- **PG**: Toss Payments 직결 (PortOne 아닌 단일 PG)
- **결제 패턴**: prepare/confirm 분리 — 서버가 orderId와 amount를 쥐는 표준 패턴
- **SMS**: `PhoneVerificationSender` 전용 포트 (`NotificationEventType` 체인과 별도)
- **회원 가입**: `PhoneOwnershipVerificationUseCase` 신규 (기존 `VerifiedGuestResolver`는 Guest upsert까지 해서 재사용 불가)

### 예약금 정책 (확정됨)

- 규칙: `DepositCalculator.of(slot) = slot.getBookingClass().getPrice() * 10 / 100`
- 근거: `docs/PRD/0001_기준_스펙/spec.md:106` — "예약금: 클래스 가격의 **10%**"
- 현재 코드(`DefaultGuest|MemberBookingService`)는 클라이언트가 `depositAmount`를 보내는 구조 → `BookingPreparer`가 서버에서 산출하도록 전환 (Task #6에서).

### 진행도

- **Phase 1 (Toss 결제)**: **백엔드 + 프론트 전환 완료**, 운영 검증 대기
  - ✅ `PaymentPort.confirm(paymentKey, orderId, amount)` 시그니처 + `PaymentConfirmResult` record
  - ✅ 도메인: `PaymentAttempt`, `PaymentContext` (ORDER/BOOKING/PASS), `PaymentAttemptStatus` (PENDING/CONFIRMED/FAILED/CANCELED) — 금액 변조 방어는 `PaymentAttempt.requireConfirmable(expectedAmount)`에 응집
  - ✅ `V32__add_payment_attempt.sql`, `V33__add_payment_key_columns.sql` — **V31이 이미 `cleanup_redundant_indexes`로 점유되어 플랜의 V31/V32를 V32/V33로 shift**. 이 번호 규약을 다음 세션에서도 유지.
  - ✅ `PaymentAttemptReaderPort`, `PaymentAttemptStorePort`, `PaymentAttemptRepository` (Repository-as-Adapter)
  - ✅ `TossPaymentsProvider` (`@Profile("prod")`), `TossPaymentsProperties`, `TossPaymentsRestClientConfig` — Basic Auth(secretKey+":") base64, `/v1/payments/confirm` · `/v1/payments/{paymentKey}/cancel`
  - ✅ `FakePaymentProvider` — `@Profile("!prod")` + `confirm()` 구현
  - ✅ `CircuitBreakerPaymentProvider.confirm()` wrapping — 서킷 브레이커 + 3초 타임아웃 자동 적용
  - ✅ 테스트 보강 — `PaymentAttempt` 금액/상태 guard, Fake/Toss confirm, CircuitBreaker confirm 보호 경계 검증 추가. `@UseCaseIT`/`@Tag("policy")` 네이밍도 테스트 지침에 맞게 정리.
  - ✅ `PaymentPrepareUseCase` / `PaymentConfirmUseCase`, context별 `PaymentPreparer` / `PaymentFulfiller` 구현
  - ✅ `BookingPreparer`가 서버 기준 예약금 10%를 산출하고, `PassPriceProperties`가 8회권 금액을 설정값으로 받음
  - ✅ `PaymentController` 신규 진입점(`/api/v1/payments/prepare`, `/api/v1/payments/confirm`) 추가
  - ✅ 기존 생성 엔드포인트 `POST /api/v1/orders`, `POST /api/v1/bookings`, `POST /api/v1/me/passes` 제거
  - ✅ 프론트 결제 전환: `frontend/src/features/payment/` (api · TossCheckout SDK 동적 로드 · types · session) + `/payments/success`·`/payments/fail` 라우트 + Pass·Order·Booking 페이지 전환 (8회권 사용 예약은 amount=0 → confirm 직호출로 PG 우회). 죽은 컴포넌트(`BookingFormStep`/`BookingSuccessCard`/`OrderSuccessCard`) 정리.
  - ✅ `application.yml`의 `app.external.payment.toss.*` 바인딩 추가 — `TOSS_SECRET_KEY` 환경변수 + base-url/타임아웃 기본값
  - 🚧 **남은 Task (다음 세션 진입점)**:
    - 결제 전환 후 문서 동기화 필요: `README.md`, `docs/PRD/0001_기준_스펙/spec.md`, `docs/PRD/0004_API_계약/spec.md`
    - 최종 빌드 + FakePaymentProvider로 주문/예약/8회권 결제 경로 end-to-end 검증 필요
- **Phase 2 (SMS 실발송)**: 0%
- **Phase 3 (회원가입 휴대폰 소유 확인)**: 0%

### 플랜 밖으로 미룬 것

HTTPS 구성, 비밀번호 복잡도, Grafana/Prometheus 인증, ADMIN 링크 UX, FakePaymentProvider `@Profile` 게이트(Phase 1에서 자연 해소), Google OAuth state 서버 검증, phone-key rate limit — 별도 트랙.

### 환경 변수 신규

- `TOSS_SECRET_KEY` (백엔드, 현재 `application.yml` 바인딩 정리 필요)
- `VITE_TOSS_CLIENT_KEY` (프론트)
- `PASS_TOTAL_PRICE` (기본 240000)

### 이 섹션 유지 규칙

- Phase 3 완료 시까지 이 섹션을 삭제하지 않는다.
- "표현 정리" 명목으로도 덮지 않는다. 내용 정리는 `~/.claude/plans/imperative-greeting-barto.md`에서 한다.

---

## 이 파일의 목적

- 다음 AI 에이전트가 세션 시작 직후 가장 먼저 읽는 문서다.
- 현재 코드 기준으로 바로 필요한 사실만 남긴다.
- 오래된 작업 이력, 세부 변경 로그, 이미 문서화된 설계 배경은 넣지 않는다.

## 우선 확인 문서

1. `README.md`
2. `plan.md`
3. 관련 `docs/PRD`
4. 관련 `docs/ADR`
5. 필요할 때만 `docs/Idea`

원칙:
- 현재 동작과 계약은 `README.md`, `docs/PRD`, `docs/ADR`를 우선한다.
- `docs/Idea`는 배경 메모다. 구현 기준 문서가 아니다.

## 현재 상태

- 현재 브랜치: `payment-integration`
- 운영 주소: `https://d36l7yi27358tl.cloudfront.net/`
- 백엔드는 6개 모듈 구조다.
  - `bootstrap`
  - `adapter-in-web`
  - `adapter-out-persistence`
  - `adapter-out-external`
  - `application`
  - `domain`
- 프론트는 `frontend/`, 운영 모니터링 설정은 `monitoring/`에 있다.

## 현재 워크트리 메모

- 워크트리가 깨끗하지 않다.
- 현재 수정 파일은 주로 문서와 `.github/workflows/deploy.yml`이다.
- 코드 쪽에는 untracked 파일 `application/src/main/java/com/personal/happygallery/application/payment/port/out/PaymentConfirmResult.java`가 있다.
- 작업 시작 전 `git status --short`로 다시 확인하고, 남의 변경은 되돌리지 않는다.

## 현재 운영 기준

- 인증 방식
  - 회원: `HG_SESSION`
  - 관리자: Bearer 세션
  - 비회원: `X-Access-Token`
- 주요 경로
  - 스토어: `/products`
  - 예약 생성: `/bookings/new`
  - 8회권 구매: `/passes/purchase`
  - 비회원 조회: `/guest`
  - 관리자: `/admin`
- 비회원 경로는 현재도 유지 중이지만, 운영상으로는 “보조 경로” 취급이다.
- 운영 배포 구조는 `CloudFront + S3 + ALB + ECS Fargate + RDS + ElastiCache Redis`다.
- `local`에서는 기본 관리자 `admin / admin1234`가 자동 생성된다.
- `local`이 아닌 환경에서는 `ADMIN_SETUP_TOKEN`으로 `/api/v1/admin/setup`을 통해 최초 관리자 계정을 만든다.
- 운영 환경에서는 클래스가 자동 생성되지 않는다. 관리자 화면에서 클래스를 먼저 등록한 뒤 슬롯을 생성한다.

## 현재 활성 목표

`plan.md` 기준 큰 정리 작업은 대부분 완료 상태다.  
지금 바로 잡아야 할 우선순위는 아래 순서로 본다.

1. 운영에 직접 영향을 주는 보안/오동작
2. 실제 사용자 경로 회귀
3. 테스트 공백
4. 구조와 문서의 불일치

즉시 진행 후보:
- 관측성 대시보드/알림 규칙 추가 보강
- `/guest` 보조 경로 유지 여부 검토
- 구현과 문서 차이 재정리

## 작업 규칙

- 문서를 수정하면 `README.md`, `HANDOFF.md`, 관련 `docs/PRD`, `docs/ADR`까지 같이 맞춘다.
- 구현 변경 시 가장 작은 관련 테스트부터 실행한다.
- Testcontainers 계열은 기본적으로 `./gradlew --no-daemon ...`를 쓴다.
- 오래된 표현을 문서에 남기지 말고, 사용자 기준 표현과 실제 코드 기준 표현을 우선한다.
- 리팩토링 전에는 `rg`로 같은 패턴이 다른 곳에도 있는지 먼저 확인한다.

## 자주 쓰는 명령

```bash
./gradlew build
./gradlew test
./gradlew :bootstrap:bootRun
./gradlew :application:policyTest
./gradlew --no-daemon :application:useCaseTest
docker compose up -d
```

## 빠른 판단 기준

- 제품 요구사항이 궁금하면 `docs/PRD/0001_기준_스펙/spec.md`
- API 계약이 궁금하면 `docs/PRD/0004_API_계약/spec.md`
- 설계 이유가 궁금하면 관련 `docs/ADR`
- 배포 구조가 궁금하면 `README.md`, `docs/Idea/0028_*`, `0029_*`, `0039_*`

## 이 파일 유지 규칙

- 다음 AI 에이전트가 바로 행동할 수 있는 정보만 남긴다.
- 이미 `README.md`, `plan.md`, `docs/PRD`, `docs/ADR`에 있는 긴 설명은 복붙하지 않는다.
- 길어지면 줄인다. 역사 기록 대신 현재 상태를 우선한다.
