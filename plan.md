# Plan

이 파일은 현재 활성 작업만 유지한다.
완료된 실행 계획은 별도 보관하지 않고 제거하며, 장기적으로 남겨야 하는 내용은 `README.md`, `HANDOFF.md`, `docs/PRD`, `docs/ADR`에 반영한다.

---

## Active Tracks

### 1. Observability Stack Upgrade

현재 상태:
- 기본 `requestId`, 구조화 로그, Actuator `health/info/metrics`, `[client-monitoring]` 로그는 이미 반영됨
- `/actuator/prometheus` 노출과 `happygallery.funnel.*` 커스텀 메트릭 추가까지는 완료됨
- Prometheus/Grafana 로컬 운영 스택과 Sentry 백엔드/프론트 기본 wiring까지 완료됨
- 남은 작업은 local scrape 확인, metric 증가 테스트, 운영 runbook 정리 중심

다음 작업:

| Task | 상태 | 범위 | 비고 |
|------|------|------|------|
| `O1-T1` | done | `app/build.gradle` | `micrometer-registry-prometheus` runtimeOnly 추가 |
| `O1-T2` | done | `application.yml` | `/actuator/prometheus` 노출 |
| `O1-T3` | done | 로컬 실행 구성 | `monitoring/prometheus.yml` + docker-compose Prometheus 서비스 추가 |
| `O1-T4` | pending | 검증 | local scrape 확인 (앱 실행 후 `localhost:9090/targets` 에서 확인) |
| `O2-T1` | done | monitoring 패키지 | `AppMetrics` 생성, `happygallery.funnel.*` 네임스페이스 |
| `O2-T2` | done | backend monitoring | `ClientMonitoringService` → `AppMetrics.incrementClientEvent()` |
| `O2-T3` | done | guest claim 완료 경로 | claim 완료 로그 경유로 `AppMetrics.incrementGuestClaimCompleted()` 반영 |
| `O2-T4` | pending | backend test | metric 증가 테스트 추가 |
| `O3-T1` | done | dashboard 구성 | Grafana provisioning (datasource + dashboard provider) + docker-compose 서비스 추가 |
| `O3-T2` | done | dashboard | `monitoring/dashboards/system.json` — HTTP rate/latency/5xx, JVM heap/GC/threads, HikariCP, CPU |
| `O3-T3` | done | dashboard | `monitoring/dashboards/funnel.json` — client event rate/totals, guest claim, conversion gauge |
| `O3-T4` | done | alert rules | `monitoring/alerts.yml` — 5xx rate, latency p95, HikariCP exhaustion, JVM heap, app down |
| `O4-T1` | done | backend | `sentry-spring-boot-starter-jakarta` 추가, GlobalExceptionHandler에서 500 에러 캡처 |
| `O4-T2` | done | frontend | `@sentry/react` 추가, API 5xx 에러에 requestId 태깅 후 캡처 |
| `O4-T3` | done | 공통 config | backend: env `SENTRY_DSN/ENVIRONMENT/RELEASE`, frontend: `VITE_SENTRY_*`, requestId context-tag |
| `O5-T1` | pending | 문서 | README/HANDOFF 동기화 후 운영 runbook 정리 잔여 |

원칙:
- 시스템 메트릭과 제품 전환 지표를 분리한다.
- `userId`, `orderId`, `phone` 같은 고유값은 metric label로 쓰지 않는다.
- 성공 funnel은 Sentry가 아니라 metric으로 본다.

### 2. Hexagonal Architecture Transition

현재 상태:
- `H1` baseline 문서와 ADR은 완료
- `H2`~`H5` 주요 전환은 완료되어 controller/batch 진입점이 `port/in` 유스케이스를 호출함
- 남은 작업은 테스트 전략과 장기 보관 문서 동기화 중심

이미 반영된 범위:
- `customer auth`, `guest claim`, `admin session`
- `booking`, `order`, `pass`, `payment`, `notification`, `product`
- 경계가 있는 서비스는 `port/in`, `port/out`, `*Adapter`로 분리했고 주요 구현체는 `Default*` 규칙으로 정리

다음 작업:

| Task | 상태 | 범위 | 비고 |
|------|------|------|------|
| `H2-T1` | done | `payment` | `PaymentPort` + adapter 완료 |
| `H2-T2` | done | `notification` | `NotificationSenderPort` + `NotificationLogReaderPort` 완료 |
| `H2-T3` | done | `customer session` | `CustomerSessionPort` + adapter 완료 |
| `H2-T4` | done | `admin session` | `AdminSessionPort` 직접 구현 완료 |
| `H3-T1` | done | `customer auth + guest claim` | infra 직접 의존 제거 완료 |
| `H3-T2` | done | `booking/order/pass/product` | pass 6개 서비스 port 전환 완료, 전 도메인 infra 직접 의존 제거 |
| `H4-T1` | done | controller | BookingCancel/Reschedule, PassPurchase UseCase 추출, 컨트롤러 전환 완료 |
| `H4-T2` | done | batch/scheduler | PassExpiryBatch, PickupExpireBatch UseCase 추출, BatchScheduler/AdminController 전환 완료 |
| `H5-T1` | done | package 정리 | 전 도메인 port/in·port/out·adapter 위치 확인 완료 — 추가 정리 불필요 |
| `H5-T2` | done | naming cleanup | UseCase 구현 7개 서비스 `Default*` rename 완료 |
| `H6-T1` | pending | 테스트 전략 | port 단위 테스트와 통합 테스트 경계 정리 |
| `H6-T2` | pending | 문서 | README/HANDOFF/ADR 후속 동기화 |

원칙:
- `서비스 = 전부 인터페이스`는 금지
- 경계가 있는 의존성만 port로 분리
- 새 구현체/rename에는 `Default*`, 기술 구현에는 `*Adapter`
- 완료된 구조 개선 내용은 ADR/README/HANDOFF로 흡수하고, 이 파일에서는 현재 backlog만 유지

### 3. Product / Ops Review

| Task | 상태 | 범위 | 비고 |
|------|------|------|------|
| `P-OPS-1` | pending | `/guest` 운영 정책 | 허브 노출 유지 기간 결정 |
| `P-OPS-2` | pending | `/orders/new` fallback | direct entry 축소 여부 결정 |
| `P-OPS-3` | pending | `/my` UX | quick tab / 정렬 / 필터 운영 피드백 반영 |

관측 기준:
- `/guest` 허브 유입
- `/orders/new` direct continue
- guest -> member CTA 클릭
- guest claim 완료
- 문의 유형

---

## Rules

- 새 실행 계획은 이 파일에만 추가한다.
- 완료된 task는 체크 후 제거하거나 간단한 완료 메모만 남기고 정리한다.
- 장기 보관 가치가 있는 설계/요구사항/운영 정보는 `docs/ADR`, `docs/PRD`, `README.md`, `HANDOFF.md`로 옮긴다.
