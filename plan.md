# Plan

이 파일은 현재 활성 작업만 유지한다.
완료된 실행 계획은 별도 보관하지 않고 제거하며, 장기적으로 남겨야 하는 내용은 `README.md`, `HANDOFF.md`, `docs/PRD`, `docs/ADR`에 반영한다.

---

## Active Tracks

### 1. Observability Stack Upgrade

현재 상태:
- 기본 `requestId`, 구조화 로그, Actuator `health/info/metrics`, `[client-monitoring]` 로그는 이미 반영됨
- Prometheus/Grafana/Sentry 기반 운영 관측성은 아직 미완료

다음 작업:

| Task | 상태 | 범위 | 비고 |
|------|------|------|------|
| `O1-T1` | pending | `app/build.gradle` | `micrometer-registry-prometheus` 추가 |
| `O1-T2` | pending | `application.yml` | `/actuator/prometheus` 노출 |
| `O1-T3` | pending | 로컬 실행 문서/구성 | Prometheus scrape 경로 정리 |
| `O1-T4` | pending | 검증 | local scrape 확인 |
| `O2-T1` | pending | monitoring 패키지 | metric 이름/label 규격 확정 |
| `O2-T2` | pending | backend monitoring | `client-events` 수신 시 counter 반영 |
| `O2-T3` | pending | `GuestClaimService` | claim 완료 counter 반영 |
| `O2-T4` | pending | backend test | metric 증가 테스트 추가 |
| `O3-T1` | pending | dashboard 문서 | Grafana 패널/PromQL 초안 |
| `O3-T2` | pending | dashboard | 시스템 메트릭 패널 |
| `O3-T3` | pending | dashboard | 제품 전환 지표 패널 |
| `O3-T4` | pending | runbook | alert 기준 정리 |
| `O4-T1` | pending | backend | Sentry 연동 |
| `O4-T2` | pending | frontend | Sentry 연동 |
| `O4-T3` | pending | 공통 config | release/environment/requestId tagging |
| `O5-T1` | pending | 문서 | 운영 runbook/README/HANDOFF 동기화 |

원칙:
- 시스템 메트릭과 제품 전환 지표를 분리한다.
- `userId`, `orderId`, `phone` 같은 고유값은 metric label로 쓰지 않는다.
- 성공 funnel은 Sentry가 아니라 metric으로 본다.

### 2. Hexagonal Architecture Transition

현재 상태:
- `H1` baseline 문서와 ADR은 완료
- `H2` 외부 경계 추출은 일부 완료
- `H3` persistence pilot도 일부 진행됨

이미 반영된 범위:
- `customer auth`, `guest claim`, `admin session`
- `booking`, `order`, `pass`, `payment`, `notification`, `product`
- 기존 서비스는 이름을 유지한 채 `port/in`, `port/out`, `*PortAdapter`를 우선 도입

다음 작업:

| Task | 상태 | 범위 | 비고 |
|------|------|------|------|
| `H2-T1` | partial_done | `payment` | `PaymentPort` 도입 후 호출부 정리 잔여 확인 |
| `H2-T2` | partial_done | `notification` | `NotificationSenderPort` 정리 잔여 확인 |
| `H2-T3` | partial_done | `customer session` | 구현체 분리 후 naming/패키지 다듬기 |
| `H2-T4` | partial_done | `admin session` | 운영 저장소 교체 가능성 기준 점검 |
| `H3-T1` | partial_done | `customer auth + guest claim` | 직접 `infra` 의존 잔여 제거 |
| `H3-T2` | partial_done | `booking/order/pass/product` | reader/store/history port 정리 마무리 |
| `H4-T1` | pending | controller | `UseCase` 경계 명시화 |
| `H4-T2` | pending | batch/scheduler | 진입점이 `UseCase`를 호출하도록 정리 |
| `H5-T1` | pending | package 정리 | `usecase`, `port/in`, `port/out`, adapter 위치 재정리 |
| `H5-T2` | pending | naming cleanup | 새 구현체/rename에 `Default*` 규칙 적용 |
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
