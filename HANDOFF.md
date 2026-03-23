# HANDOFF.md
> 다음 세션을 위한 인수인계 문서.
> 작성 시점: 2026-03-22 (배포 준비 문서/인프라 반영, 문서 인덱스 보정 상태)

---

## 프로젝트 요약

**happyGallery** — 오프라인 공방의 온라인 쇼핑몰 + 체험 예약 시스템 (Spring Boot 4.0.2 / Java 21 / MySQL 8)

- 빠른 진입 문서: `README.md`
- 현재 활성 계획: `plan.md`
- 핵심 스펙: `docs/PRD/0001_기준_스펙/spec.md`
- API 계약 문서: `docs/PRD/0004_API_계약/spec.md`
- 의사결정 기록: `docs/ADR/`
- 회원 스토어 차기 PRD 초안: `docs/PRD/0002_회원_스토어_전환/spec.md`
- 기준 확인 순서: `HANDOFF.md -> plan.md -> docs/PRD/0001_기준_스펙/spec.md -> docs/PRD/0004_API_계약/spec.md -> docs/ADR/*`

---

## 현재 브랜치 / 워크트리 상태

- 권장 작업 브랜치: `codex/work-20260319-000329`
- 최근 작업:
  - 마이페이지 컴포넌트 분해 + 공통 UI 정리 — `/my` 대시보드의 hero/stats/claim/목록 섹션을 전용 컴포넌트로 분리했고, 상품/주문 타입 라벨을 `frontend/src/shared/lib/labels.ts`로 모았으며, 앱 최상단 `ErrorBoundary`와 홈 상품 목록 에러 표시를 추가해 프론트 장애 대응을 정리
  - 관리자 인증/세션 로컬 보정 — `AdminAuthFilter`의 `X-Admin-Key` 비교를 `MessageDigest.isEqual`로 바꾸고 `/admin/auth/**`만 인증 예외로 좁혔으며, local 프로필에서는 `HG_SESSION` secure cookie를 끄도록 `app.session.secure-cookie=false`를 추가했다
  - 예약 도메인/배치 정리 — 예약 변경·취소·결석의 `BOOKED` 상태 검증을 `BookingStatus.requireBooked()`로 모았고, D-1/당일 예약 리마인드 배치는 `BatchExecutor`를 사용하도록 맞췄으며, nginx에는 기본 보안 응답 헤더를 추가했다
  - 1차 배포 준비 정리 — `ADR-0028`에 prod 로그 민감 데이터 마스킹, `nginx` SPA fallback + `/api` 리버스 프록시, `application-prod.yml` forwarded headers/rate-limit trust 설정, Grafana 관리자 비밀번호 환경변수 외부화 결정을 정리했고, `docs/Idea/0027_*`에 Tomcat `internal-proxies` 재검토 조건을 기록
  - 중복 PasswordEncoder 설정 제거 — `CryptoConfig`와 동일한 `passwordEncoder` 빈을 다시 등록하던 `PasswordEncoderConfig`를 제거해 Spring 테스트 컨텍스트의 `BeanDefinitionOverrideException` 충돌을 해소
  - 알림 비동기 테스트 대기 정리 — `NotificationLogTestHelper`를 Awaitility 기반으로 전환했고, 알림 로그 저장 완료를 기다리는 테스트는 공통 대기 유틸을 계속 사용하도록 정리했다. 적용 판단 기준은 `docs/Idea/0026_비동기_테스트_대기_Awaitility_우선/idea.md`에 기록했다
  - bootJar 패키징 검토 메모 추가 — `:app:bootJar`에 `common`, `domain`은 포함되지만 `infra`는 포함되지 않는 현재 구조와, 실행 전용 조립 모듈 분리 후보를 `docs/Idea/0025_bootJar_패키징과_Bootstrap_모듈_분리/idea.md`에 기록
  - 픽업 마감 알림 배치 + 실알림 어댑터 기반 추가 — 매시간 `pickupDeadlineAt` 기준 2시간 이내 `PICKUP_READY` 주문에 알림을 보내는 배치를 추가했고, prod 프로필에서는 카카오 알림톡/NHN SMS 실제 sender를 사용하고 비운영에서는 fake sender를 유지하도록 분리했으며 관련 외부 설정 키와 문서 인덱스를 갱신
  - 테스트 전략 ADR 승격 — 커버리지 수치보다 비즈니스 검증/문서화와 최소 테스트 세트를 우선하는 기준, 테스트/문서의 절대량 자체를 줄여 장기 관리 비용을 통제해야 한다는 원칙, 유스케이스/도메인 정책/직렬화·역직렬화 테스트 분류를 `ADR-0027`로 승격하고 기존 `docs/Idea/0003_*`는 배경 메모로 축소
  - `@UseCaseIT` 고정 Clock 기준선 추가 — test 컨텍스트에서 Asia/Seoul 고정 `Clock`을 `@Primary`로 주입하고, `PassPurchaseUseCaseIT`·`BookingCancelUseCaseIT`·`BookingRescheduleUseCaseIT`·`PassCreditUsageUseCaseIT`·`RefundExecutionServiceUseCaseIT`의 벽시계(`now()`) 의존을 주입 Clock 기준으로 정리해 시간 경계 테스트를 결정적으로 맞춤
  - guest 소유 8회권 제거 — `PassPurchase.guest`/guest claim pass/guest pass booking을 제거하고, 만료 알림을 회원 기준으로 전환했으며 `pass_purchases.guest_id` 제거 migration(V21)과 관련 테스트·문서를 함께 정리
  - `@UseCaseIT` 주입 기준 보정 — 웹이나 유스케이스 인터페이스로 호출할 수 있는 기능은 구현 클래스 대신 `MockMvc` 또는 유스케이스 인터페이스 타입으로 검증하고, service 직접 주입은 테스트 데이터를 준비하는 코드에만 남기도록 `PassPurchaseUseCaseIT`부터 정리
  - graceful shutdown 운영 기준 문서화 — `ADR-0025`에 Spring graceful shutdown 30초, 알림 `ThreadPoolTaskExecutor` drain 정책, PG timeout executor 2초 종료 정책을 정리
  - 문서 인덱스/인수인계 보정 — `README.md` Idea 목록을 실제 파일 기준으로 정리하고, `HANDOFF.md` 프론트 경로/문의 흐름 요약을 구현 상태에 맞게 갱신
  - 배치 마이그레이션 검토 메모 추가 — `docs/Idea/0017_Spring_Batch_마이그레이션_검토/idea.md`에 현행 커스텀 배치 유지 판단과 Spring Batch 재검토 조건을 기록
  - spec 분리 2차 — core `docs/PRD/0001_기준_스펙/spec.md`에서 API 카탈로그/에러 계약을 `docs/PRD/0004_API_계약/spec.md`로, 시스템 경계·상태/스키마 기준과 관리자 인증·런타임 기준을 `ADR-0022`, `ADR-0023`으로 분리
  - PRD 경량화 — `docs/PRD/0001_기준_스펙/spec.md`에서 테스트 전략/`SoftAssertions.assertSoftly` 규칙과 관리자 인증 확장 검토를 분리했고, 현재 테스트 전략은 `ADR-0027`, 관리자 인증 확장 검토는 `docs/Idea/0004_*`에서 관리한다
  - 문서 체계 재정리 — 활성 실행 계획은 루트 `plan.md`로 통합, 완료된 `docs/1Pager` 실행 계획 문서는 제거, `docs/POC/0001_결제_제공자_CircuitBreaker_적용/poc.md`를 추가하고 `README.md` 문서 목록을 현재 구조 기준으로 재작성
  - Redis 기반 세션/레이트리밋 전환 완료 — 회원 세션을 Spring Session + Redis(`HG_SESSION`)로 전환하고 `user_sessions` 의존을 제거했으며, 관리자 Bearer 세션과 `RateLimitFilter`도 Redis 저장소로 옮겨 다중 인스턴스 대응을 맞춤
  - 통합 테스트 프로필/컨텍스트 정리 완료 — `@UseCaseIT`가 `test` 프로파일을 기본 사용하도록 정리하고, `AdminSlotUseCaseIT`를 포함한 필터 검증 테스트를 수동 `MockMvc` 조립 패턴으로 맞춰 불필요한 컨텍스트 분리를 줄임
  - 운영 관측성 3차 구현 완료 — `docker-compose.yml`에 Prometheus/Grafana 서비스를 추가하고 `monitoring/`에 scrape 설정, alert rule, datasource/provisioning, system/funnel 대시보드를 반영했으며, backend 500 예외와 frontend API 5xx 에러를 Sentry로 캡처하도록 정리
  - 전수점검 5트랙 완료 — 보안 강화(OTP/admin 기본값/actuator), 예약 조회/리마인더 복구(member/claimed 포함), guest token hardening(SHA-256/X-Access-Token), 테스트 커버리지 회복(me API/admin filter/batch), 아키텍처 수렴(product/notification/payment/booking/order 포트 추출, BookingCreationSupport, N+1 수정)
  - 상품 Q&A / 1:1 문의 1차 추가 — `product_qna`, `inquiry` 스키마(V19), 공개 상품 Q&A 조회/비밀글 확인, 회원 Q&A 작성/1:1 문의 작성·조회, 관리자 답변 화면/엔드포인트 추가
  - 헥사고날 전환 pilot 2차 진행 — booking cancel/reschedule, pass purchase/expiry batch, pickup expiry batch 경계에 `port/in` 유스케이스를 추가하고 controller/batch 진입점을 해당 포트로 전환, `NotificationLogReaderPortAdapter`를 도입하고 주요 구현체 이름을 `Default*`로 정리
  - 운영 모니터링 2차 구현 완료 — 프론트 주요 guest/member 전환 지점의 `[client-monitoring]` 로그 수집에 더해 `AppMetrics`를 도입해 `happygallery.funnel.client_event`, `happygallery.funnel.guest_claim_completed` 메트릭을 기록하고 `/actuator/prometheus`를 노출하도록 정리
  - `/orders/new` 직접 진입 확인 단계 추가 — 상품 상세에서 상품/수량을 받아 내려온 경우는 바로 진행하고, query 없이 직접 진입한 `/orders/new`는 “보조 경로” 안내 뒤에 계속 버튼을 눌러야 수동 비회원 다중 상품 주문을 진행하도록 정리
  - guest lookup 허브 추가 완료 — `/guest` 진입 페이지를 추가하고, 상단 utility bar와 홈은 direct guest lookup 대신 `/guest` 허브를 우선 노출하도록 정리
  - guest route 운영 카피 정리 완료 — `/guest/orders`, `/guest/bookings`, 홈 lookup 패널, guest 성공 CTA에서 guest 경로를 “조회용 보조 경로”로 명확히 표시하고, 로그인/회원가입 후 `/my` claim으로 이어지는 전환 문구를 강화
  - 로그인 직후 회원 상태 전역 동기화 완료 — `CustomerAuthProvider`를 도입해 로그인/회원가입 후 상단 네비가 새로고침 없이 즉시 `로그아웃` 상태로 반영되도록 정리하고, `P8-9`에 즉시 상태 전환 검증 추가
  - Playwright 핵심 시나리오 구조 정리 — `frontend/tests/e2e/p8-smoke.spec.ts` 단일 파일을 사용자 여정 기준 4개 파일(`admin-product-order`, `guest-booking-pass`, `member-self-service`, `guest-claim-onboarding`)로 분리하고, 공통 UI 선택자와 보조 함수는 별도 `ui-support.ts`로 정리
  - `/my` 목록 고도화 완료 — `/my/orders`, `/my/bookings`, `/my/passes`에 quick status tab, 정렬, 요약 chip을 추가해 회원 이력 탐색 흐름을 한 단계 정리
  - 회원 온보딩 polish 완료 — 로그인/회원가입 페이지를 storefront/member 문맥에 맞는 2열 레이아웃으로 정리했고, `redirect`·`claim`·회원가입 시 미리 채워 넣는 값(`name`/`phone`)도 로그인/회원가입 전환 링크에서 유지되도록 보강했다. `/my?claim=1` 진입 뒤 모달을 닫아도 후속 claim 안내 카드는 계속 남긴다
  - `/my` 목록 필터 확장 완료 — `/my/orders`, `/my/bookings`, `/my/passes`에 상태 필터와 검색을 추가했고, 회원 8회권 구매 완료 CTA도 `/my/passes`로 맞췄다
  - member/guest success flow 고도화 완료 — guest 주문/예약 성공 화면에서 `회원가입/로그인 -> /my claim` 경로를 직접 안내하고, 회원가입 페이지는 휴대폰/이름을 미리 채운 상태와 claim 안내를 함께 보여 준다. `/my?claim=1`은 claim 모달을 자동으로 연다
  - `/my` 세부 라우트 확장 완료 — `/my/orders`, `/my/bookings`, `/my/passes` 목록 페이지 추가, 대시보드는 최근 5건 + `전체 보기` 구조로 정리, 회원 상세 페이지의 back link도 각 목록 기준으로 맞춤
  - guest/member lookup UX polish 완료 — 상단 utility bar와 홈 lookup CTA에서 `회원 내 정보`/`비회원 조회` 구분을 명확히 하고, `/guest/orders`·`/guest/bookings`를 안내형 카드 + 액션 버튼 구조로 정리, guest 주문/예약 완료 카드에도 `/guest/**` 조회와 회원 claim 진입 버튼을 함께 배치
  - member self-service polish 완료 — `/my` 로그인 게이트를 회원/비회원 진입이 분명한 대시보드형 카드로 정리하고, 로그인 후에는 주문·예약·8회권 요약 통계/다음 예약/guest claim 진입을 한 화면에 배치, `/my/orders/:id`·`/my/bookings/:id` 상세 헤더와 CTA도 회원용 카피 기준으로 정리
  - legacy guest route 정리 완료 — `/orders/detail`, `/bookings/manage` redirect alias 제거, guest 조회 경로를 `/guest/orders`, `/guest/bookings`로 단일화하고 관련 README/PRD/1Pager/P8 문서를 기본 경로 기준으로 갱신
  - P8-3 핵심 브라우저 테스트 안정화 완료 — Playwright가 고정 시각 슬롯과 충돌하지 않도록 E2E 보조 코드에서 기존 admin 슬롯 시작 시각을 피하는 고유 슬롯 시간을 선택하도록 보강했고, `P8-5`의 모호한 `주문하기` selector도 같이 정리해 1~8 시나리오를 다시 통과시켰다
  - U6 guest claim 브라우저 자동화 완료 — Playwright `P8-8`을 추가했고, guest 주문·예약 생성 후 같은 번호의 회원이 `/my`에서 휴대폰 재인증과 선택 claim을 수행하는 시나리오를 자동화했다. 보조 코드에는 잠긴 전화번호 검증과 커스텀 회원가입 전화번호 지원도 추가했다
  - U3 비회원 주문 보조 경로 보강 — 상품 상세의 `비회원 주문하기`가 `/orders/new?productId=&qty=`로 이동하며 선택한 상품/수량을 미리 채우고, 기존 비회원 주문 페이지에서 그대로 수정/추가 주문할 수 있게 정리했으며 `P8-4`도 이 경로 기준으로 갱신
  - U5 회원 셀프서비스 완료 — `/my/bookings/:id` 회원 예약 상세 페이지를 추가하고 예약 변경/취소 UI를 연결했으며, `/guest/orders`·`/guest/bookings`를 기본 비회원 조회 경로로 정리했다. `GET /api/v1/me/bookings/{id}` 상세 조회에서는 slot/class 즉시 조회도 보강했다
  - U5 guest claim 2차 완료 — `/api/v1/me/guest-claims/{preview,verify,claim}` 추가, 회원 휴대폰 재인증 후 같은 번호의 guest 주문/예약을 선택 이전하는 마이페이지 모달 반영, 회원 전화번호의 하이픈/숫자-only 포맷 차이 흡수
  - U3 storefront / 상품 상세 1차 완료 — 홈과 네비게이션을 상점형 IA로 재구성하고, 상품 상세를 구매 중심 레이아웃으로 정리했다. `/orders/new`는 기존 비회원 주문 보조 경로로 명시했다
  - U6 운영 반영 / E2E 3차 완료 — Playwright 핵심 시나리오 1~9를 통과시켰고, guest/member/claim 혼합 시나리오와 guest 성공 화면 -> 회원가입 -> claim 모달 자동 진입까지 정리했다. 관리자 로그인 토큰 캐시와 고객 세션 쿠키를 미리 준비하는 보조 코드도 반영했고, README/P8/U6/PRD/HANDOFF 문서를 함께 맞췄다
  - U5 회원 셀프서비스 1차 — `/my`, `/my/orders/:id`, 회원 주문/예약/8회권 목록 조회, 회원 주문 상세, 회원 예약/8회권 생성과 `/api/v1/me/**` 기반 흐름 확장
  - U4 제출 직전 인증 단계 + 주문 진입 전환 — `/products/:id` 회원 주문과 `/bookings/new`·`/passes/purchase`의 member/guest 인증 선택 분기를 정리했고, 기존 guest 조회 경로와 함께 유지했다
  - U1 고객 인증 기반 — `User` 엔티티, `CustomerAuthUseCase(DefaultCustomerAuthService)` + `CustomerAuthFilter`(Spring Session + Redis 기반 `HG_SESSION`), `/api/v1/auth/{signup,login,logout}` + `GET /api/v1/me`, 프론트 `LoginPage`/`SignupPage`/Layout 로그인 상태 표시, V14 migration, rate limit(customer-login 10/min, customer-signup 5/min)
  - CR-P7 배송 흐름 및 운영 이력 확장 — 배송 전이 API 3종 (prepare-shipping, mark-shipped, mark-delivered), 주문 이력 조회 API, expectedShipDate write guard, 프론트 배송 액션/이력 패널, spec/plan/HANDOFF 문서 동기화
  - CR-P6 계약/감사/무결성 후속 — fulfillment.status FE 정합, admin principal 세션 전환, fulfillments.order_id unique, convertToPickup stale 데이터 제거, resume-production HTTP test, 프론트 중복 정리, README/PRD/ADR/E2E 문서 정합화
  - 문서 정합화 — spec.md, ADR-0013, ADR-0014 상태명·Fulfillment 구조 반영
  - CR-P5 장기 리팩토링 — DELAY_REQUESTED 재개 흐름, Fulfillment 단일성, *_REFUNDED 상태명 변경, Fulfillment.status 제거, PG 환불 패턴 통합, local 실패 주입 API 범위 제한
  - P10 관측성/운영 준비 — Actuator 노출 정책, 에러 응답 requestId 포함, 배치 MDC requestId 주입
  - P9 프로덕션 인증 계층 — BCrypt 기반 관리자 로그인, UUID 세션 토큰, `X-Admin-Key` 개발용 보조 인증 토글
  - P8 E2E 시나리오 검증 — local 환불 실패 hook 추가, 기본 관리자 계정 정합화, 실제 로컬 핵심 브라우저 시나리오 1~9 통과
- 프론트 생성물(`node_modules`, `dist`, `*.tsbuildinfo`)은 `frontend/.gitignore` 기준으로 추적 제외
- 최근 검증:
  - `./gradlew clean :app:test --no-daemon` 통과
  - `cd frontend && npm run build` 통과 (`@sentry/react` 포함)
  - `docker compose config` 통과 (Prometheus/Grafana compose wiring 확인)
  - `cd frontend && npm run e2e -- --grep "P8-9"` 통과
  - `cd frontend && npm run e2e` 통과 (핵심 브라우저 시나리오 1~9)
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.customer.CustomerGuestClaimUseCaseIT` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.customer.CustomerAuthUseCaseIT` 통과
  - `cd frontend && npm run e2e -- --grep "P8-8"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(2|3|4|8|9)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(6|7)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(2|4)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-7"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(4|7|8)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-6"` 통과 (U3 핵심 회원 주문 경로)
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderProductionUseCaseIT` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.booking.LocalBookingClassSeedServiceTest` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.infra.payment.FakePaymentProviderTest --tests com.personal.happygallery.app.web.admin.LocalRefundFailureControllerTest --tests com.personal.happygallery.app.web.admin.AdminLoginUseCaseIT` 통과
  - `curl -s http://127.0.0.1:8080/actuator/health` 응답 확인

---

## 프론트 진행 상황

기존 프론트 이행 계획은 정리했고, 현재는 구현 상태와 활성 백로그만 유지한다.

### 현재 프론트 진입 경로

- `/` — 서비스 홈 (진입 카드)
- `/products` — 상품 목록
- `/products/:id` — 상품 상세 + 회원 주문 진입
- `/login` — 고객 로그인
- `/signup` — 고객 회원가입
- `/my` — 회원 마이페이지 (`내 주문`, `내 예약`, `내 8회권`, `비회원 이력 가져오기`)
- `/my/orders` — 회원 주문 전체 목록
- `/my/orders/:id` — 회원 주문 상세
- `/my/bookings` — 회원 예약 전체 목록
- `/my/bookings/:id` — 회원 예약 상세 + 변경/취소
- `/my/passes` — 회원 8회권 전체 목록
- `/my/inquiries` — 회원 1:1 문의 목록
- `/my/inquiries/new` — 회원 1:1 문의 작성
- `/bookings/new` — 예약 생성 (member/guest 제출 직전 인증 선택 단계)
- `/passes/purchase` — 8회권 구매 (회원 전용, 비로그인 시 로그인 리다이렉트)
- `/guest` — 비회원 조회 안내 허브
- `/guest/orders` — 비회원 주문 조회
- `/guest/bookings` — 비회원 예약 조회/변경/취소
- `/orders/new` — 기존 비회원 주문 보조 경로 (`productId`, `qty` 미리 채우기 지원, 주소를 직접 입력해 들어오면 계속 버튼을 한 번 더 눌러야 진행)
- `/admin` — 관리자 (사용자명/비밀번호 로그인, Bearer 토큰 인증)

---

## 현재 활성 계획

현재 남아 있는 실행 백로그는 루트 `plan.md`만 기준으로 본다.

핵심 트랙:
1. 관측성 스택 고도화
2. guest/member 운영 정책 리뷰와 `/my` 운영 피드백 반영

완료된 트랙 (plan.md 전수점검):
- Track 1: 공개/관리자 인증 보안 강화 — OTP 응답 코드 제거, admin 기본값 안전화, actuator 포트 분리
- Track 2: 예약 조회/리마인더 복구 — admin 예약 목록 LEFT JOIN, bookerType(GUEST/MEMBER) 도입, reminder batch guest/member 분기
- Track 3: Guest token hardening — SHA-256 해시 저장(V17), X-Access-Token 헤더 통일, V18 backfill
- Track 4: 테스트 커버리지 회복 — me/bookings·orders·passes IT, admin filter 검증, pass credit/reminder batch 보강
- Track 5: 아키텍처 수렴 — product/notification/payment/booking/order/admin query 포트 추출, BookingCreationSupport 공통화, N+1 수정, guest claim 분리

완료된 프론트 플랜, 폴리시 플랜, 리팩토링 플랜, 코드리뷰 후속 plan 문서는 정리했고,
장기 보관 가치가 있는 내용만 `README.md`, `HANDOFF.md`, `docs/PRD`, `docs/ADR`에 흡수했다.

---

## 알아야 할 것들

### Spring Boot 4.0 특이사항
- `@UseCaseIT`는 현재 `@AutoConfigureMockMvc(addFilters = false)` 기반으로 유지 중
- `@UseCaseIT`의 `test` 컨텍스트는 `TestcontainersConfig`에서 Asia/Seoul 고정 `Clock`을 `@Primary`로 제공한다. 시간 관련 테스트 데이터는 `LocalDateTime.now(clock)` 또는 `LocalDate.now(clock)` 기준으로 맞추는 편이 안전하다.
- `@SpringBootTest` 컨텍스트에서 `ObjectMapper` autowire 불가 → JSON 문자열 직접 구성
- Codex 샌드박스에서는 Gradle JVM 명령이 `FileLockContentionHandler` 소켓 생성 제한에 걸릴 수 있어, 테스트와 `:app:bootRun`은 처음부터 권한 상승 실행으로 처리하는 편이 안정적
- 동일하게 `gh pr *`, 원격 `git fetch/push/pull`, Docker 컨테이너 제어, Playwright 브라우저 설치/실행, 워크스페이스 밖 경로 쓰기처럼 반복적으로 막혔던 작업도 샌드박스 재시도 없이 처음부터 권한 상승 실행으로 처리한다.

### 프론트 공통 패턴
- 고객 인증: `useCustomerAuth()` 훅에서 `/api/v1/me`로 세션 확인 (HttpOnly 쿠키 자동 포함), `login`/`signup`/`logout` 제공
- 회원가입 전화번호는 프론트에서 숫자만 정규화해 전송하고, guest claim은 회원 전화번호의 하이픈/숫자-only 포맷 차이를 모두 허용한다.
- Layout에서 `useCustomerAuth()`로 로그인 상태에 따라 "로그인"/"사용자명+로그아웃" 표시
- `/products/:id`, `/bookings/new` 는 회원이면 세션 기준으로 바로 제출하고, 비회원이면 제출 직전에 인증 선택 단계를 연다.
- `/passes/purchase` 는 회원 전용이며, 비로그인 시 로그인 페이지로 리다이렉트한다.
- 상품 상세는 회원 전용 Product Q&A 섹션을 포함하고, 비밀글은 비밀번호 검증 후 상세를 확인한다.
- 상품 상세의 `비회원 주문하기`는 `/orders/new?productId=&qty=` 로 이동해 선택 상품과 수량을 기존 비회원 주문 보조 경로에 미리 담아둔다. query 없이 직접 진입한 `/orders/new`는 계속 버튼을 누르는 확인 단계를 먼저 거친다.
- `/my` 에서 `비회원 이력 가져오기` 모달을 열면, `phoneVerified=false` 회원은 같은 번호로 재인증 후 preview를 보고 주문/예약을 선택 claim 할 수 있다.
- guest 주문/예약 성공 화면의 회원가입/로그인 CTA는 `redirect=/my?claim=1` 로 이어지고, `/my`는 이 쿼리로 claim 모달을 자동으로 연다.
- 로그인/회원가입 페이지는 `redirect`와 `claim` 문맥을 유지하고, 회원가입은 guest 성공 화면에서 넘어온 `name`/`phone` 미리 채운 값도 이어받는다.
- `/my?claim=1` 로 진입한 뒤 자동 오픈된 claim 모달을 닫아도, 대시보드의 claim 카드에서 후속 안내와 재진입 버튼을 계속 노출한다.
- `/my/bookings/:id` 는 회원 예약 상세/변경/취소 화면이며, 비회원 조회는 `/guest/bookings` 로 분리한다.
- `/my/orders`, `/my/bookings`, `/my/passes` 는 검색, 상태 필터, quick tab, 정렬을 제공한다.
- `/my/inquiries`, `/my/inquiries/new` 는 회원 전용 1:1 문의 목록/작성 경로이며, 관리자는 `/admin`에서 전체 문의 조회/답변을 처리한다.
- `/guest` 를 비회원 조회 시작 경로로 노출하고, `/guest/orders`, `/guest/bookings` 는 기본 비회원 조회 경로이자 생성 후 확인용 보조 경로로 유지한다.
- 현재 운영 권장안은 `/guest` 허브, `/guest/orders`, `/guest/bookings`, `/orders/new` 직접 진입 확인 단계를 그대로 유지하는 것이다. 회원 경로를 안정화한 뒤 2~4주 동안 사용량과 문의 유형을 보고 비회원 보조 경로를 줄일지 결정한다.
- `/api/v1/monitoring/client-events` 는 guest/member 주요 전환 이벤트를 requestId가 포함된 `[client-monitoring]` 로그로 남기고, 동시에 `happygallery.funnel.client_event`, `happygallery.funnel.guest_claim_completed` 메트릭을 누적한다. 현재 수집 범위는 `/guest` 허브 진입, `/orders/new` 직접 진입 뒤 계속, guest 성공/조회 화면의 회원 전환 CTA, `/my` claim 모달 오픈, guest claim 완료다.
- frontend는 `@sentry/react`로 API 5xx 에러를 캡처하고, backend는 `sentry-spring-boot-4-starter` + `GlobalExceptionHandler`에서 예상치 못한 500 예외를 캡처한다. 두 경로 모두 가능하면 `requestId`를 태그로 남긴다.
- 관리자 API 401 처리: `onAuthError` 콜백을 AdminPage에서 모든 하위 컴포넌트에 전달
- 관리자 인증: `useAdminKey()` 훅에서 사용자명/비밀번호 로그인 → UUID 세션 토큰을 `sessionStorage` (`hg_admin_token`)에 저장, 이후 `Authorization: Bearer {token}` 헤더 사용
- Playwright 핵심 시나리오는 관리자 Bearer 토큰과 고객 `HG_SESSION` 쿠키를 backend API로 미리 만들어 로그인 rate limit과 UI 초기화 타이밍 영향을 줄였다.
- Playwright 테스트 파일은 현재 사용자 여정 기준 4개 파일(`admin-product-order.smoke.spec.ts`, `guest-booking-pass.smoke.spec.ts`, `member-self-service.smoke.spec.ts`, `guest-claim-onboarding.smoke.spec.ts`)로 나뉘어 있고, 시나리오 번호 `P8-1`~`P8-9`는 그대로 유지한다.
- `P8-8`은 guest 주문·예약을 만든 뒤, 같은 번호의 회원이 `/my` 모달에서 재인증 후 claim 하는 흐름을 검증한다.
- `P8-9`는 guest 주문 성공 화면에서 회원가입으로 넘어가 휴대폰/이름 미리 채운 값과 `/my` claim 모달 자동 오픈을 검증한다.
- local 기본 관리자 계정: `admin` / `admin1234` (`LocalAdminSeedService`, `@Profile("local")`)
- 프로덕션 관리자 계정은 Flyway에 포함하지 않고 별도 초기 생성 절차를 전제로 한다.
- 개발/테스트에서는 `X-Admin-Key` 보조 인증 가능 (`enable-api-key-auth=true`)
- 회원 세션은 Spring Session + Redis를 사용하며, 쿠키 이름 `HG_SESSION` 계약은 유지한다. `CustomerAuthController`는 로그인/회원가입 시 세션의 `customerUserId`를 기록하고, `CustomerAuthFilter`는 Spring Session filter 이후 실행되며 세션 사용자 ID로 DB 조회를 수행한다.
- 관리자 Bearer 세션(`AdminSessionStore`)과 `RateLimitFilter`도 Redis를 사용한다. 로컬 기본 Redis 주소는 `localhost:6379`, docker compose 앱 컨테이너는 `REDIS_HOST=redis`로 연결한다.
- API 에러 응답은 필요 시 `requestId`를 포함하고, 배치 로그도 `batch-*` requestId를 같이 남긴다.
- 슬롯 생성: 공개 `/classes` API로 클래스 드롭다운 제공 (API 없을 시 ID 직접 입력 폴백)
- 주문 총액: `OrderItemsForm`에서 상품 가격 × 수량으로 실시간 합계 표시
- `BookingFormStep`의 결제 방식 라디오는 명시적 `id`를 써서 라벨 접근성을 보장

### 로컬 실행 메모
- `local` 프로필 `:app:bootRun`은 `classes` 테이블이 비어 있으면 향수/우드/니트 기본 클래스 3종을 seed한다.
- 로컬 부팅이나 `docker compose up -d` 전에 Redis(`localhost:6379`)가 필요하다. compose에는 `redis` 서비스가 추가되어 있고, 통합 테스트는 Testcontainers Redis를 함께 기동한다.
- `docker compose up -d --build` 뒤에는 `nginx`가 `http://localhost` 에서 frontend `dist` 정적 파일을 서빙하고 `/api` 요청을 app 컨테이너로 프록시한다.
- `local` 프로필에서 `http://localhost:8080/actuator/prometheus` 로 JVM/HTTP 메트릭과 `happygallery.funnel.*` 커스텀 메트릭을 함께 노출한다.
- `docker compose up -d prometheus grafana` 로 로컬 관측성 스택을 따로 띄울 수 있고, 포트는 각각 `9090`, `3001`이다.
- Grafana 로그인은 `GRAFANA_ADMIN_USER`/`GRAFANA_ADMIN_PASSWORD` 환경 변수를 사용하고, 사용자명 기본값은 `admin`이다.
- Sentry는 backend `SENTRY_DSN/SENTRY_ENVIRONMENT/SENTRY_RELEASE`, frontend `VITE_SENTRY_DSN/VITE_SENTRY_ENVIRONMENT/VITE_SENTRY_RELEASE` 환경 변수를 사용한다.
- 알림 sender는 `!prod`에서 fake sender, `prod`에서 카카오 알림톡/NHN SMS 실제 sender를 사용한다. 실제 운영 발송에는 `KAKAO_*`, `SMS_*` 환경 변수가 필요하다.
- clean DB 기준으로도 P8 guest/member 핵심 브라우저 시나리오 1~9를 바로 실행할 수 있다.
- `DELETE /api/v1/admin/dev/payment/refunds/fail-next`로 훅을 비우고, `POST /api/v1/admin/dev/payment/refunds/fail-next`로 다음 환불 1회 실패를 arm할 수 있다.
  요청 바디에 `orderId`를 넣으면 특정 주문으로 범위를 좁힐 수 있다.

### 테스트 실행

```bash
./gradlew test
./gradlew :app:test --tests "*.SomeIT"
./gradlew :app:policyTest
./gradlew --no-daemon :app:useCaseTest
./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.admin.AdminSlotUseCaseIT
cd frontend && npm run build
cd frontend && npm run e2e:install
cd frontend && npm run e2e
```

### 미해결 과제
- 로컬 `bootRun` 전 `happygallery-app` 컨테이너가 떠 있으면 8080 충돌 발생
- 현재 `:app:bootJar`는 `common`, `domain`은 포함하지만 `infra`는 포함하지 않는다. `app` 단독 실행 산출물로 유지할지, bootstrap 모듈을 분리해 최종 조립을 맡길지 검토 필요 (`docs/Idea/0025_bootJar_패키징과_Bootstrap_모듈_분리/idea.md`)
- PG 환불 패턴 중복 → 실 PG 연동 시 RefundExecutor로 통합 예정
- ~~공개 주문 상세 `fulfillment.status` 계약 drift 정리 필요~~ (CR-P6에서 FE/BE 정합)
- ~~`X-Admin-Id` 헤더 의존 제거 전까지 운영 이력의 admin 식별자가 null/위조 가능~~ (CR-P6에서 Bearer 세션 attribute 기반으로 전환)
- ~~`fulfillments.order_id` unique 부재로 단일 fulfillment 가정이 DB에서 아직 보장되지 않음~~ (CR-P6에서 V13 migration으로 unique 제약 추가)
- ~~배송 상태 enum(`SHIPPING_PREPARING`, `SHIPPED`, `DELIVERED`)은 있으나 운영 API/화면 흐름과 `expectedShipDate` write guard는 미완성~~ (CR-P7에서 배송 전이 API 3종, 이력 조회, write guard 구현)
- ~~`DELAY_REQUESTED` → 재개 경로 없음~~ (CR-P5에서 `resumeProduction` 추가)
- ~~Fulfillment.status와 Order.status 이중 관리~~ (CR-P5에서 Fulfillment.status 제거, Order.status가 단일 소스)

---

## ADR 목록

| 번호 | 주제 |
|------|------|
| ADR-0001 | 핵심 스키마 |
| ADR-0002 | 상태 전이 가드 |
| ADR-0003 | 슬롯 동시성 전략 |
| ADR-0004 | 슬롯 관리 구현 |
| ADR-0005 | 게스트 예약 구현 |
| ADR-0006 | 예약 변경 결정 |
| ADR-0007 | 예약 취소 결정 |
| ADR-0008 | 결제 인터페이스 추상화 |
| ADR-0009 | 예약금 결제 정책 |
| ADR-0010 | 8회권 구매/만료 결정 |
| ADR-0011 | 8회권 사용/소모/환불 결정 |
| ADR-0012 | 상품/재고 구현 결정 (§8.1) |
| ADR-0013 | 주문 승인 모델 결정 (§8.2) |
| ADR-0014 | 예약 제작 주문 구현 결정 (§8.3) |
| ADR-0015 | 운영 로그 구조화 및 비즈니스 예외 스택 최적화 |
| ADR-0016 | API URI 버저닝 전략 도입 |
| ADR-0017 | 필터 기반 API 처리율 제한 도입 |
| ADR-0018 | 환불 이력 저장 트랜잭션 분리(REQUIRES_NEW) |
| ADR-0019 | 비밀번호 해시 정책 (Salt + Key Stretching) |
| ADR-0020 | 결제 환불 외부 호출 보호를 위한 CircuitBreaker 도입 |
| ADR-0021 | 기존 app/domain/infra 구조 위에서 점진적 헥사고날 전환 채택 |
| ADR-0022 | 시스템 경계, 상태 모델, 데이터 모델 기준선 |
| ADR-0023 | 관리자 인증 및 런타임 운영 기준선 |
| ADR-0024 | Guest access token SHA-256 해시 저장 + X-Access-Token 헤더 전환 |
| ADR-0025 | Graceful Shutdown 및 Executor Drain 정책 |
| ADR-0026 | 통합 테스트 프로파일 및 Testcontainers 기준선 |
| ADR-0027 | 테스트 전략 — 최소 고가치 검증 우선 |
| ADR-0028 | 1차 배포 준비 — 알림 실 연동, 로그 마스킹, 배포 인프라 |
