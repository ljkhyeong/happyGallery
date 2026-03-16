# happyGallery

오프라인 공방의 **온라인 쇼핑몰 + 체험 예약 시스템**.
상품 주문, 클래스 예약, 8회권 패스, 관리자 운영을 하나의 플랫폼에서 처리한다.

- **백엔드**: Spring Boot 4.0.2 / Java 21 / MySQL 8
- **프론트엔드**: Vite / React 19 / TypeScript / Bootstrap
- **구조**: Gradle 멀티 모듈(`app` · `domain` · `infra` · `common`) + `frontend/` 워크스페이스

---

## 문서 목록

### PRD (Product Requirements Document)

| 문서 | 경로 | 설명 |
|------|------|------|
| [Core MVP Specification](docs/PRD/0001_spec/spec.md) | `docs/PRD/0001_spec/` | 전체 시스템의 기능 요구사항 정의서 (단일 진실 원천) |
| [Member Store Transition](docs/PRD/0002_member_store_transition/spec.md) | `docs/PRD/0002_member_store_transition/` | 회원 인증·스토어 전환을 위한 차세대 요구사항 |

<details>
<summary>주요 요약</summary>

- **Core Spec**: 예약(슬롯·보증금·환불), 주문(승인 워크플로), 패스(8회 크레딧), 상품/재고, 비회원 휴대폰 인증, 관리자 Bearer 세션 등 MVP 전체 계약을 정의
- **Member Store**: HttpOnly 쿠키 + 세션 테이블 기반 회원 인증, `/my` 셀프서비스(주문·예약·패스), 비회원 이력 명시적 claim 워크플로, guest→member 전환 모니터링 정의
</details>

---

### 1Pager (전략 계획)

| 문서 | 경로 | 설명 |
|------|------|------|
| [Project Plan](docs/1Pager/0000_project_plan/plan.md) | `docs/1Pager/0000_project_plan/` | 초기 프로젝트 로드맵 (기초 스냅샷) |
| [Code Review Plan](docs/1Pager/0001_code_review_plan/plan.md) | `docs/1Pager/0001_code_review_plan/` | 유닛 기반 코드 리뷰 전략 (U1–U12) |
| [Refactoring Plan](docs/1Pager/0002_refactoring_plan/plan.md) | `docs/1Pager/0002_refactoring_plan/` | 구조 개선 10단위 (R1–R10, 완료) |
| [Frontend Plan](docs/1Pager/0003_frontend_plan/plan.md) | `docs/1Pager/0003_frontend_plan/` | React/Bootstrap UI 구현 (B1–B4, F0–F9, 완료) |
| [Polish Plan](docs/1Pager/0004_polish_plan/plan.md) | `docs/1Pager/0004_polish_plan/` | MVP 이후 품질 강화 (P1–P10, 완료) |
| [P8 E2E Checklist](docs/1Pager/0005_p8_e2e_checklist/plan.md) | `docs/1Pager/0005_p8_e2e_checklist/` | Playwright E2E 스모크 검증 (Smoke 1–9) |
| [Code Review Followups](docs/1Pager/0006_code_review_followups/plan.md) | `docs/1Pager/0006_code_review_followups/` | 리뷰 후속 위험 정리 (P1–P7, 완료) |
| [Member Store Transition](docs/1Pager/0007_member_store_transition/plan.md) | `docs/1Pager/0007_member_store_transition/` | 회원 인증 기반 구축 및 롤아웃 (U1–U6, 완료) |
| [Me Controller Refactoring](docs/1Pager/0008_me_controller_refactoring/plan.md) | `docs/1Pager/0008_me_controller_refactoring/` | 서비스 계층 분리 및 N+1 해소 (W1–W9) |
| [Observability Stack Upgrade](docs/1Pager/0009_observability_stack_upgrade/plan.md) | `docs/1Pager/0009_observability_stack_upgrade/` | Prometheus·Grafana·Sentry 메트릭 확장 (O1–O5) |
| [Hexagonal Architecture Transition](docs/1Pager/0010_hexagonal_architecture_transition/plan.md) | `docs/1Pager/0010_hexagonal_architecture_transition/` | Port/Adapter 패턴 점진 도입 (H1–H6) |

<details>
<summary>주요 요약</summary>

- **Project Plan**: 저장소 구성 → DB 스키마 → 에러 시스템 → 예약 MVP → 패스/주문 → 배치 → 관리자 → E2E 검증까지 전체 마일스톤
- **Code Review**: 세션당 1유닛; 버그·스펙 불일치 중심; "수정 완료 / 후속 이슈 / 테스트 리스크" 3단 리포트
- **Refactoring**: Order 상태 캡슐화, 에러 매핑, 예약 통합, 알림 폴백, 배치 템플릿, DTO 정리, 시간 경계, 테스트 픽스처 등 10단위 구조 개선
- **Frontend**: Vite + React + TanStack Query 기반 스토어프론트, 회원/관리자 화면, Playwright E2E 스모크
- **Polish**: 스펙 동기화, 홈페이지, 폼 검증, 반응형, 관리자 운영 화면, 프로덕션 인증, 관측성 기초
- **Member Store Transition**: 회원 인증 기초 → 계약 모델 → 스토어프론트 → 지연 인증 게이트 → 셀프서비스 → 롤아웃/E2E 까지 6단계
  - 하위 상세 문서: [01_customer_auth_foundation](docs/1Pager/0007_member_store_transition/01_customer_auth_foundation.md) · [02_identity_model_and_contracts](docs/1Pager/0007_member_store_transition/02_identity_model_and_contracts.md) · [03_storefront_and_product_detail](docs/1Pager/0007_member_store_transition/03_storefront_and_product_detail.md) · [04_deferred_verification_and_checkout](docs/1Pager/0007_member_store_transition/04_deferred_verification_and_checkout.md) · [05_member_self_service_and_guest_lookup](docs/1Pager/0007_member_store_transition/05_member_self_service_and_guest_lookup.md) · [06_rollout_migration_and_e2e](docs/1Pager/0007_member_store_transition/06_rollout_migration_and_e2e.md)
- **Hexagonal Transition**: 기존 모듈 유지하며 `port/in`(UseCase) · `port/out`(Port) 점진 도입; 교체 가능한 경계에만 인터페이스 적용
- **Observability Upgrade**: RequestId·JSON 로그·Actuator 기초 위에 Prometheus 카운터, Grafana 대시보드, Sentry 알림 추가
</details>

---

### ADR (Architecture Decision Record)

| 문서 | 경로 | 설명 |
|------|------|------|
| [ADR-0001: Core Schema](docs/ADR/0001_core-schema/adr.md) | `docs/ADR/0001_core-schema/` | 16개 핵심 테이블 설계 및 마이그레이션 전략 |
| [ADR-0002: State Transition Guards](docs/ADR/0002_state-transition-guards/adr.md) | `docs/ADR/0002_state-transition-guards/` | 도메인 객체 내 상태 전이 가드 및 예외 체계 |
| [ADR-0003: Slot Concurrency](docs/ADR/0003_slot-concurrency-strategy/adr.md) | `docs/ADR/0003_slot-concurrency-strategy/` | 슬롯 용량 동시성 제어 (SELECT FOR UPDATE) |
| [ADR-0004: Slot Management](docs/ADR/0004_slot-management-impl-decisions/adr.md) | `docs/ADR/0004_slot-management-impl-decisions/` | 슬롯 중복 검사, OSIV 비활성, 버퍼 비활성화 |
| [ADR-0005: Guest Booking](docs/ADR/0005_guest-booking-impl-decisions/adr.md) | `docs/ADR/0005_guest-booking-impl-decisions/` | 비회원 예약 흐름 및 휴대폰 인증 구현 |
| [ADR-0006: Booking Reschedule](docs/ADR/0006_booking-reschedule-decisions/adr.md) | `docs/ADR/0006_booking-reschedule-decisions/` | 예약 변경 정책 및 슬롯 교환 전략 |
| [ADR-0007: Booking Cancel](docs/ADR/0007_booking-cancel-decisions/adr.md) | `docs/ADR/0007_booking-cancel-decisions/` | 예약 취소 환불 정책 (D-1 00:00 기준) |
| [ADR-0008: Payment Provider](docs/ADR/0008_payment-provider-abstraction/adr.md) | `docs/ADR/0008_payment-provider-abstraction/` | 결제 공급자 추상화 레이어 설계 |
| [ADR-0009: Deposit Payment](docs/ADR/0009_deposit-payment-policy/adr.md) | `docs/ADR/0009_deposit-payment-policy/` | 보증금 10% 결제 정책 |
| [ADR-0010: Pass Purchase & Expiry](docs/ADR/0010_pass-purchase-expiry-decisions/adr.md) | `docs/ADR/0010_pass-purchase-expiry-decisions/` | 8회권 구매·90일 만료·7일 전 리마인더 |
| [ADR-0011: Pass Credit Usage](docs/ADR/0011_pass-credit-usage-decisions/adr.md) | `docs/ADR/0011_pass-credit-usage-decisions/` | 패스 크레딧 차감/복원 원장(ledger) 모델 |
| [ADR-0012: Product Inventory](docs/ADR/0012_product-inventory-decisions/adr.md) | `docs/ADR/0012_product-inventory-decisions/` | 상품 재고 관리 및 낙관적 잠금 |
| [ADR-0013: Order Approval Model](docs/ADR/0013_order-approval-model/adr.md) | `docs/ADR/0013_order-approval-model/` | 주문 승인/거절 워크플로 및 24시간 SLA |
| [ADR-0014: Production Order](docs/ADR/0014_production-order-decisions/adr.md) | `docs/ADR/0014_production-order-decisions/` | 제작 주문 상태 전이 (제작→배송→픽업) |
| [ADR-0015: Observability & Logging](docs/ADR/0015_observability-logging-and-business-exception/adr.md) | `docs/ADR/0015_observability-logging-and-business-exception/` | 로깅 표준, 비즈니스 예외 분류, RequestId 추적 |
| [ADR-0016: API Versioning](docs/ADR/0016_api-versioning-strategy/adr.md) | `docs/ADR/0016_api-versioning-strategy/` | URI 기반 API 버전 관리 (`/api/v1/**`) |
| [ADR-0017: Rate Limiting](docs/ADR/0017_filter-rate-limiting/adr.md) | `docs/ADR/0017_filter-rate-limiting/` | IP·엔드포인트별 필터 기반 Rate Limit |
| [ADR-0018: Refund Log REQUIRES_NEW](docs/ADR/0018_refund-log-requires-new/adr.md) | `docs/ADR/0018_refund-log-requires-new/` | 환불 로그 독립 트랜잭션 전파 전략 |
| [ADR-0019: Password Hashing](docs/ADR/0019_password-hashing-policy/adr.md) | `docs/ADR/0019_password-hashing-policy/` | BCrypt 기반 비밀번호 해싱 정책 |
| [ADR-0020: Payment Circuit Breaker](docs/ADR/0020_payment-provider-circuit-breaker/adr.md) | `docs/ADR/0020_payment-provider-circuit-breaker/` | Resilience4j 기반 결제 Circuit Breaker |
| [ADR-0021: Hexagonal Transition](docs/ADR/0021_hexagonal-architecture-transition/adr.md) | `docs/ADR/0021_hexagonal-architecture-transition/` | Port/Adapter 패턴 도입 파일럿 전략 |

<details>
<summary>아키텍처 결정 요약</summary>

**데이터 모델**
- 16개 핵심 테이블 단일 마이그레이션 · Enum은 VARCHAR · 타임스탬프 DATETIME(6)
- 슬롯 UNIQUE(class_id, start_at) · inventory/bookings에 `@Version` 낙관적 잠금

**동시성 제어**
- 슬롯 용량: `SELECT FOR UPDATE` 비관적 잠금 (최대 8명, 즉시 실패가 재시도보다 안전)
- 예약/주문/패스: `@Version` 낙관적 잠금 (충돌 드묾)

**상태 전이**
- 도메인 객체 내부에 가드 메서드 (`OrderStatus.requireApprovable()`, `SlotCapacity.checkAvailable()`)
- HTTP 상태 매핑: 409(충돌/용량), 422(정책 위반), 404(미존재)

**결제·환불**
- 결제 공급자 추상화 + Resilience4j Circuit Breaker (3초 타임아웃, 50% 실패 임계)
- 보증금 10% · 환불 로그는 `REQUIRES_NEW` 독립 트랜잭션
- 24시간 미승인 주문 자동 환불 배치

**예약 정책**
- 비회원 휴대폰 인증 → 예약 생성 · D-1 00:00(Asia/Seoul) 이후 환불 불가
- 변경 시 기존 슬롯 해제 → 새 슬롯 점유 단일 트랜잭션

**패스·재고**
- 8회 크레딧 · 90일 유효 · 7일 전 만료 리마인더
- 원장(ledger) 기반 크레딧 차감/복원 · 상품 재고 낙관적 잠금

**인프라·운영**
- API 경로 `/api/v1/**` · Bearer 세션 인증 · BCrypt 해싱
- RequestId MDC 주입 · IP/엔드포인트 Rate Limit
- 기존 모듈 유지하며 Port/Adapter 점진 도입 (교체 가능 경계에만 인터페이스)
</details>

---

### Idea (검토 중인 아이디어)

| 문서 | 경로 | 설명 |
|------|------|------|
| [JSON + Generated Column](docs/Idea/0001_json-generated-column-consideration/idea.md) | `docs/Idea/0001_json-generated-column-consideration/` | 가변 속성이 3–5개 초과 시 JSON 컬럼 + MySQL Generated Column 패턴 검토 |
| [Bulkhead (Resilience4j)](docs/Idea/0002_bulkhead-resilience4j-consideration/idea.md) | `docs/Idea/0002_bulkhead-resilience4j-consideration/` | 외부 호출 격리를 위한 Resilience4j Bulkhead 패턴 검토 |
| [Circuit Breaker Rollout](docs/Idea/0003_external-call-circuit-breaker-rollout/idea.md) | `docs/Idea/0003_external-call-circuit-breaker-rollout/` | 결제 외 외부 호출(알림 등)에 Circuit Breaker 확대 적용 검토 |

---

## 저장소 구조

```
happyGallery/
├── app/               # Spring Boot 진입점, 컨트롤러, 서비스, 배치, 통합 테스트
├── domain/            # 엔티티, 상태 전이, 정책 등 핵심 비즈니스 규칙
├── infra/             # JPA 리포지토리, 결제/알림 등 외부 연동 구현
├── common/            # 공통 예외, 시간 유틸, 공용 타입
├── frontend/          # Vite + React + TypeScript 프론트엔드
├── docs/
│   ├── PRD/           # 제품 요구사항 정의서
│   ├── 1Pager/        # 전략 계획서
│   ├── ADR/           # 아키텍처 결정 기록
│   └── Idea/          # 검토 중인 아이디어
├── HANDOFF.md         # 현재 세션 인수인계
└── CLAUDE.md          # AI 에이전트 작업 지침
```

---

## 로컬 실행

### 요구사항

- Java 21
- Node.js 20+
- Docker / Docker Compose

### 백엔드

```bash
# MySQL만 Docker로 띄우고 앱은 로컬 실행
docker compose up -d mysql
./gradlew :app:bootRun

# 또는 MySQL + 앱 컨테이너 함께 실행
docker compose up -d --build
```

`local` 프로필로 실행하면 `classes` 테이블이 비어있을 때 기본 클래스 3종(향수/우드/니트)을 자동 seed 한다.

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

- 개발 서버: `http://localhost:3000`
- `/api` 요청은 Vite proxy로 `http://localhost:8080`에 연결

### 주요 환경 변수

| 변수 | 용도 |
|------|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 데이터베이스 연결 |
| `ADMIN_API_KEY` / `ADMIN_ENABLE_API_KEY_AUTH` | 관리자 API Key 인증 |
| `PAYMENT_TIMEOUT_MILLIS` / `PAYMENT_CB_*` | 결제 타임아웃 및 Circuit Breaker |
| `RATE_LIMIT_TRUST_FORWARDED` | Rate Limit 프록시 신뢰 |

---

## 빌드와 검증

### 백엔드

```bash
./gradlew build                   # 전체 빌드
./gradlew test                    # 전체 테스트
./gradlew :app:policyTest         # 정책 테스트
./gradlew :app:useCaseTest        # 유스케이스 통합 테스트
```

### 프론트엔드

```bash
cd frontend
npm run build            # 프로덕션 빌드
npm run e2e:install      # Playwright 브라우저 설치
npm run e2e              # E2E 스모크 테스트
```

---

## 문서 우선순위

1. `HANDOFF.md` — 현재 세션 인수인계
2. `docs/PRD/0001_spec/spec.md` — 기준 스펙
3. `docs/ADR/*` — 관련 아키텍처 결정
4. `docs/1Pager/*` — 도메인별 계획
