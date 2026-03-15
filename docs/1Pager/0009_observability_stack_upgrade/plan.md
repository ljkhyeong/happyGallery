# Observability Stack Upgrade Plan

현재 P10에서 requestId, 구조화 로그, Actuator `health/info/metrics`는 이미 정리됐다.  
다만 운영자가 기대하는 `Actuator + Prometheus + Grafana + Sentry` 체계와,
회원 스토어 전환에서 필요한 제품 흐름 지표는 아직 한 단계 더 구체화가 필요하다.

이 문서는 현재의 `[client-monitoring]` 임시 로그 계측을
정식 운영 관측성 스택으로 올리기 위한 후속 플랜이다.

---

## 1. 목표

- 서버/애플리케이션 상태 메트릭은 `Actuator + Micrometer + Prometheus`로 수집한다.
- 운영 대시보드는 `Grafana` 기준으로 표준화한다.
- 프론트/백엔드 오류 추적은 `Sentry`로 분리한다.
- guest/member 전환 흐름 같은 제품 지표는 low-cardinality 메트릭으로 남긴다.
- requestId 기반 로그 추적은 유지하되, “제품 판단용 숫자”는 로그 grep이 아니라 메트릭/대시보드로 보게 만든다.

---

## 2. 현재 상태

이미 구현됨:
- `RequestIdFilter` 기반 `X-Request-Id` 발급/반환
- `prod` JSON 로그, `local` 텍스트 로그
- 에러 응답 `requestId` 포함
- 배치 MDC requestId
- Actuator 웹 노출: `health`, `info`, `metrics`
- 회원 스토어 전환용 `/api/v1/monitoring/client-events` + `[client-monitoring]` 로그

아직 없음:
- `micrometer-registry-prometheus`
- `/actuator/prometheus` 노출
- Grafana 대시보드/패널/경보 기준
- Sentry frontend/backend 연동
- `[client-monitoring]` 이벤트의 counter/timer화

---

## 3. 핵심 원칙

- 시스템 메트릭과 제품 지표를 구분한다.
  - 시스템 메트릭: latency, error rate, JVM, DB pool, batch
  - 제품 지표: `/guest` 허브 유입, `/orders/new` direct continue, guest → member CTA, claim 완료
- 제품 지표도 Prometheus 계열로 넣되, label cardinality는 낮게 유지한다.
- `userId`, `orderId`, `phone` 같은 고유값은 metric label로 쓰지 않는다.
- Sentry는 오류/예외 추적용이다. 성공 funnel 측정 수단으로 사용하지 않는다.
- `[client-monitoring]` 로그는 1차 fallback으로 유지하되, Grafana 대시보드가 검증되면 운영 판단은 메트릭 기준으로 전환한다.

---

## 4. 작업 단위

| 단위 | 성격 | 선행 | 핵심 산출물 |
|------|------|------|-------------|
| `O1` | 백엔드/인프라 | 없음 | Prometheus registry, `/actuator/prometheus`, scrape 가능 상태 |
| `O2` | 백엔드 | `O1` | guest/member 전환 지표의 Micrometer counter화 |
| `O3` | 인프라/문서 | `O1`, `O2` | Grafana 대시보드, alert 기준, 운영 조회 쿼리 |
| `O4` | 프론트+백엔드 | 없음 | Sentry frontend/backend 연동, release/environment tagging |
| `O5` | 문서/운영 | `O1`~`O4` | README/HANDOFF/운영 가이드, dashboard/runbook |

---

## 5. Task 운영 규칙

- 한 에이전트는 기본적으로 task 1개만 맡는다.
- task는 가능한 한 disjoint write scope를 가진다.
- `O1-T1`, `O1-T2`처럼 같은 상위 단위 안에서도 선행 task와 병렬 task를 구분한다.
- task 완료 보고는 `변경 파일 / 핵심 결정 / 실행 검증 / 남은 리스크` 형식으로 남긴다.
- `O2` 이후 task는 low-cardinality 원칙을 반드시 지킨다.

---

## 6. 단위별 상세

### O1. Metrics Foundation

목표:
- backend에 `micrometer-registry-prometheus`를 추가한다.
- `management.endpoints.web.exposure.include`에 `prometheus`를 포함한다.
- `local`에서 Prometheus가 scrape 가능한지 확인한다.

주요 수정 후보:
- `app/build.gradle`
- `app/src/main/resources/application.yml`
- 필요 시 `docker-compose.yml` 또는 로컬 observability 보조 compose

완료 기준:
- `GET /actuator/prometheus`가 정상 응답한다.
- 기본 JVM / http server / hikari 메트릭이 scrape 된다.

최소 검증:
- `curl http://localhost:8080/actuator/prometheus`
- Prometheus target `UP`

#### O1 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `O1-T1` | Prometheus registry 의존성 추가 | `app/build.gradle` | 없음 | 불가 |
| `O1-T2` | `/actuator/prometheus` 노출 설정 | `app/src/main/resources/application.yml`, 필요 시 `application-local.yml` | `O1-T1` | 불가 |
| `O1-T3` | local scrape 경로 정리 | `docker-compose.yml` 또는 observability 보조 구성, README 초안 | `O1-T2` | 제한적 |
| `O1-T4` | 기초 메트릭 검증 | 테스트/헬스 체크 명령, 필요 시 스모크 문서 | `O1-T2` | `O1-T3`와 병행 가능 |

task 완료 기준:
- `O1-T1`: backend가 Prometheus registry를 포함해 빌드된다.
- `O1-T2`: `/actuator/prometheus`가 노출된다.
- `O1-T3`: local에서 scrape 대상 구성이 재현 가능하다.
- `O1-T4`: curl 또는 target 기준으로 scrape 가능 여부를 확인했다.

### O2. Product Funnel Metrics

목표:
- 현재 `[client-monitoring]` 로그 이벤트를 low-cardinality metric으로 승격한다.
- 운영자가 로그 grep 없이 funnel 수치를 볼 수 있게 만든다.

권장 방식:
- 기존 `/api/v1/monitoring/client-events`는 유지하되, 수신 시 `Micrometer Counter`를 함께 증가시킨다.
- `GuestClaimService`의 claim 완료 이벤트도 같은 계열 counter로 연결한다.

권장 metric 예시:
- `happygallery_guest_lookup_hub_view_total`
- `happygallery_guest_order_direct_continue_total`
- `happygallery_guest_member_cta_click_total`
- `happygallery_guest_claim_modal_open_total`
- `happygallery_guest_claim_completed_total`

권장 label 예시:
- `source`
- `target`
- `authenticated`

금지:
- `path` free-text 전체값 label 사용
- `userId`, `orderId`, `bookingId`, `phone` label 사용

완료 기준:
- member store 운영 판단에 필요한 숫자를 Prometheus query로 바로 볼 수 있다.
- 기존 `[client-monitoring]` 로그와 대시보드 수치가 큰 차이 없이 맞는다.

최소 검증:
- event API 호출 후 `/actuator/prometheus`에서 counter 증가 확인
- guest claim 완료 후 counter 증가 확인

#### O2 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `O2-T1` | metric 이름/label 규격 확정 | `app` 모니터링 패키지, 문서 메모 | `O1` | 불가 |
| `O2-T2` | client-events 수신 시 counter 반영 | `app/src/main/java/.../monitoring/**` | `O2-T1` | 불가 |
| `O2-T3` | guest claim 완료 metric 반영 | `GuestClaimService`와 관련 monitoring service | `O2-T1` | `O2-T2`와 병행 가능 |
| `O2-T4` | metric 노출 테스트 추가 | `app/src/test/java/.../monitoring/**` | `O2-T2`, `O2-T3` | 불가 |
| `O2-T5` | 임시 `[client-monitoring]` 로그와 metric 병행 정책 문서화 | README/HANDOFF/U6 | `O2-T2`, `O2-T3` | `O2-T4`와 병행 가능 |

task 완료 기준:
- `O2-T1`: metric 명과 label이 low-cardinality 원칙에 맞게 고정됐다.
- `O2-T2`: client-events API 호출 시 metric이 증가한다.
- `O2-T3`: guest claim 완료도 같은 계열 metric으로 집계된다.
- `O2-T4`: 자동 검증으로 metric 증가를 확인한다.
- `O2-T5`: 로그와 metric의 역할 분담이 문서화됐다.

### O3. Grafana Dashboard And Alerting

목표:
- 운영자용 대시보드를 만든다.
- “상태 메트릭”과 “제품 전환 지표”를 한 화면에서 보게 한다.

대시보드 최소 패널:
- HTTP 5xx rate
- p95 / p99 latency
- Hikari active / pending
- batch success/failure count
- `/guest` 허브 유입
- `/orders/new` direct continue
- guest → member CTA click
- claim 완료 수
- claim 완료율 추정 패널

경보 최소 후보:
- HTTP 5xx 급증
- DB pool saturation
- batch 실패 연속 발생
- claim 완료율 급락

완료 기준:
- 운영자가 “장애인지, UX 이탈인지”를 1개의 Grafana 보드에서 1차 구분할 수 있다.

#### O3 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `O3-T1` | 대시보드 패널 목록/PromQL 초안 작성 | dashboard 정의 문서 또는 JSON 초안 | `O1`, `O2` | 불가 |
| `O3-T2` | 시스템 메트릭 패널 구성 | Grafana dashboard JSON | `O3-T1` | 가능 |
| `O3-T3` | 제품 전환 지표 패널 구성 | Grafana dashboard JSON | `O3-T1` | 가능 |
| `O3-T4` | alert 후보와 임계치 문서화 | runbook 또는 dashboard 관련 문서 | `O3-T2`, `O3-T3` | 불가 |
| `O3-T5` | local/dev에서 패널 값 검증 | Grafana/Prometheus 실행 메모, 검증 결과 | `O3-T2`, `O3-T3` | `O3-T4`와 병행 가능 |

task 완료 기준:
- `O3-T1`: 패널과 쿼리 목록이 고정됐다.
- `O3-T2`: 시스템 상태 패널이 보인다.
- `O3-T3`: guest/member 전환 지표 패널이 보인다.
- `O3-T4`: 운영자가 볼 경보 기준이 정리됐다.
- `O3-T5`: 대시보드가 실제 수치를 읽는지 확인했다.

### O4. Sentry Integration

목표:
- 프론트와 백엔드의 예외 추적을 Sentry로 연결한다.
- 오류는 Sentry, 메트릭은 Prometheus/Grafana로 역할을 분리한다.

백엔드 범위:
- Sentry SDK 연동
- `environment`, `release`, `requestId` tagging
- PII/민감정보 제외 정책 명시

프론트 범위:
- Vite + React Sentry SDK
- environment/release tagging
- source map 업로드 전략 정리
- `X-Request-Id`와 함께 cross-reference 가능한 수준의 context 연결

완료 기준:
- 프론트/백엔드 오류가 Sentry 프로젝트에서 환경별로 분리되어 보인다.
- 운영자가 requestId 기반 서버 로그와 Sentry event를 함께 추적할 수 있다.

#### O4 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `O4-T1` | backend Sentry 연동 | `app/build.gradle`, backend config, exception path | 없음 | `O1`과 병행 가능 |
| `O4-T2` | frontend Sentry 연동 | `frontend` 설정, build config | 없음 | `O1`과 병행 가능 |
| `O4-T3` | release/environment/requestId tagging 정리 | frontend/backend 공통 config | `O4-T1`, `O4-T2` | 불가 |
| `O4-T4` | source map / deploy 연계 방식 문서화 | frontend build/deploy 문서 | `O4-T2` | 가능 |
| `O4-T5` | test event 및 오류 추적 검증 | Sentry test event 문서와 검증 결과 | `O4-T3` | `O4-T4`와 병행 가능 |

task 완료 기준:
- `O4-T1`: backend 오류가 Sentry로 들어간다.
- `O4-T2`: frontend 오류가 Sentry로 들어간다.
- `O4-T3`: requestId와 환경 태그가 추적 가능하다.
- `O4-T4`: source map/release 전략이 정리됐다.
- `O4-T5`: 실제 test event로 동작을 확인했다.

### O5. Runbook And Handoff

목표:
- 관측성 사용법을 문서에 남긴다.
- “어디서 무엇을 봐야 하는지”를 운영 문서로 고정한다.

포함 문서:
- `README.md`
- `HANDOFF.md`
- 필요 시 신규 ADR
- 필요 시 운영용 1Pager 또는 runbook 문서

문서에 반드시 들어갈 항목:
- local/dev/prod 관측성 구성 차이
- Prometheus scrape 엔드포인트
- Grafana 대시보드 위치
- Sentry 프로젝트/환경 규칙
- `[client-monitoring]` 로그를 언제까지 유지할지

#### O5 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `O5-T1` | README 운영 메모 최신화 | `README.md` | `O1` 이상 | 가능 |
| `O5-T2` | HANDOFF 다음 세션 기준 정리 | `HANDOFF.md` | `O1` 이상 | 가능 |
| `O5-T3` | 운영 runbook/1Pager 정리 | 신규 runbook 또는 기존 1Pager | `O3`, `O4` | 불가 |
| `O5-T4` | `[client-monitoring]` 로그 유지/축소 정책 문서화 | PRD/ADR/HANDOFF | `O2`, `O3` | `O5-T3`와 병행 가능 |

task 완료 기준:
- `O5-T1`: 로컬/운영 실행과 관측성 사용법이 README에 있다.
- `O5-T2`: 다음 세션에서 바로 이어받을 수 있다.
- `O5-T3`: 운영자가 대시보드와 Sentry를 어디서 봐야 하는지 문서화됐다.
- `O5-T4`: 임시 로그를 언제 줄일지 기준이 남아 있다.

---

## 7. 권장 순서

1. `O1` Metrics Foundation
2. `O2` Product Funnel Metrics
3. `O3` Grafana Dashboard And Alerting
4. `O4` Sentry Integration
5. `O5` Runbook And Handoff

병렬 가능:
- `O1`과 `O4`는 병렬 가능
- `O3`는 `O1`, `O2` 이후가 효율적

---

## 8. 병렬 배정 예시

- 에이전트 A: `O1-T1` ~ `O1-T4`
- 에이전트 B: `O4-T1` ~ `O4-T5`
- 에이전트 C: `O2-T1` ~ `O2-T5` (`O1` 완료 이후)
- 에이전트 D: `O3-T1` ~ `O3-T5` (`O2` 완료 이후)
- 에이전트 E: `O5-T1` ~ `O5-T4` (문서 전담)

---

## 9. 다른 에이전트에게 넘길 때 주의할 점

- P10은 “기본 관측성” 완료 상태다. 이번 플랜은 그 위에 observability stack을 올리는 2차 작업이다.
- 현재 member store 전환에는 `/api/v1/monitoring/client-events`와 `[client-monitoring]` 로그가 이미 들어가 있다.
- 다른 에이전트는 이 임시 경로를 바로 제거하지 말고, Prometheus counter가 검증된 뒤 축소 여부를 판단해야 한다.
- 제품 지표 metric은 low-cardinality가 핵심이다. `path` 전체값, 사용자 식별자, 토큰, 주문번호는 label로 쓰면 안 된다.

---

## 10. 완료 기준

- `/actuator/prometheus`가 노출되고 scrape 된다.
- member store 운영 판단 지표가 Grafana에서 수치로 보인다.
- 프론트/백엔드 오류가 Sentry에서 환경별로 추적된다.
- README/HANDOFF 기준으로 운영자가 어떤 화면/툴을 봐야 하는지 명확하다.

---

## 11. 최소 검증

- `./gradlew --no-daemon :app:test --tests ...`
- `cd frontend && npm run build`
- `curl http://localhost:8080/actuator/prometheus`
- Prometheus target 확인
- Grafana 패널 수치 확인
- Sentry test event 확인
