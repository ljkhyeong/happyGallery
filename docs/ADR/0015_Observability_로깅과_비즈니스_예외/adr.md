# ADR-0015: 운영 로그 구조화 및 비즈니스 예외 스택 최적화

**날짜**: 2026-03-06  
**최종 갱신**: 2026-07-27
**상태**: Accepted

---

## 컨텍스트

운영 환경에서 텍스트 로그는 검색·집계 자동화가 어렵고,
비즈니스 예외가 빈번한 경로에서 스택트레이스 수집 비용이 불필요하게 발생할 수 있다.

현재 시스템은 요청 단위 `requestId`를 MDC에 저장하고 있으므로,
구조화 로그(JSON)와 결합하면 관측성이 크게 개선된다.

---

## 결정 사항

### 1. 운영(`prod`) 로그는 JSON 포맷으로 출력한다

- `logback-spring.xml`에서 `prod` 프로필 시 `LogstashEncoder` 사용
- MDC 포함(`requestId` 포함)하여 필드 기반 검색 가능
- 서비스 식별 필드(`service`)를 공통 필드로 추가
- 클라이언트 `X-Request-Id`는 UUID 또는 `[A-Za-z0-9._:-]` 문자로 이루어진 최대 64자 토큰만
  재사용한다. 그 밖의 값은 서버 UUID로 교체해 로그·응답 헤더 크기와 형식을 제한한다.

### 2. 비운영(`!prod`)은 텍스트 로그를 유지한다

- 로컬/개발 환경의 가독성을 위해 기존 형태 유지

### 3. 로그 레벨 운영 정책을 프로필별로 고정한다

- `TRACE`/`DEBUG`:
  - 로컬 개발 환경(`local`)에서만 활성화
  - SQL 파라미터 바인딩, 상세 호출 흐름 추적 등 디버깅 목적에 한정
- `INFO`:
  - 운영 환경(`prod`) 기본값
  - 애플리케이션 시작/종료, 배치 시작/완료, 결제 완료 같은 주요 비즈니스 이벤트만 기록
  - 단순 디버깅 정보는 `INFO`에 기록하지 않음
- `WARN`:
  - 즉시 장애는 아니지만 잠재적 위험이 있는 경우 사용
  - 예: 파싱 실패 후 기본값 대체, 외부 연동 일시 실패 후 재시도
- `ERROR`:
  - 운영자가 즉시 확인해야 하는 장애 상황만 기록
  - 요청 식별자(`requestId`)와 에러 코드를 함께 남겨 추적 가능하게 유지

### 4. `HappyGalleryException` 계층은 스택트레이스 생성을 비활성화한다

- `super(message, null, false, false)` 사용
- 비즈니스 예외(4xx)는 메시지/코드 중심 처리로 충분하며, 고빈도 경로의 오버헤드를 줄인다

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 운영 로그 검색/집계/대시보드 구성이 쉬워짐 (ELK/DataDog 친화) |
| 장점 | 비즈니스 예외 생성 비용 감소 |
| 단점 | 비즈니스 예외의 상세 스택 디버깅 정보는 줄어듦 |
| 대응 | 시스템 예외는 기존대로 스택트레이스를 유지하고, 비즈니스 예외는 코드/메시지/요청ID로 추적 |

---

## 구현 반영

- `bootstrap/src/main/resources/logback-spring.xml` 추가
- `bootstrap/build.gradle`에 `logstash-logback-encoder` 런타임 의존성 추가
- `domain/error/HappyGalleryException` 생성자 변경
- `AppMetrics`의 `happygallery.payment.confirm.reconciliation_required` 카운터와
  `PaymentConfirmReconciliationRequired` Prometheus critical 알림으로 결제 수동 대사 필요 상태를 즉시 노출한다.
  `monitoring/alerts.yml`을 Compose와 k3s의 단일 경보 원본으로 사용한다.
  k3s는 `sync-prometheus-alerts.sh`가 만든 `prometheus-alerts.generated.yml`을 자급적인 ConfigMap으로 렌더링하고,
  `validate.sh`가 원본과 생성 파일 및 최종 ConfigMap의 drift를 차단한다.
- k3s Prometheus는 private Alertmanager로 모든 rule을 전달한다. Alertmanager는 critical/warning 반복 간격을 분리하고 저장소 밖 Kubernetes Secret의 `url_file`로 외부 HTTPS generic webhook에 전달한다. Prometheus·Alertmanager·Grafana는 각각 Retain PVC를 사용하고, Grafana는 외부 Ingress 없이 cluster 내부 Viewer와 port-forward 접근만 허용한다. 같은 노트북 전체가 중단되는 상황은 별도 외부 uptime 감시가 담당한다.
- `@BatchJob`은 고정된 영문 `id`와 한국어 로그 이름을 함께 가진다. `BatchLoggingAspect`는 실행 결과(`succeeded`, `partial`, `failed`), 항목 결과, 소요 시간과 마지막 정상 완료 Unix 시각을 Micrometer에 기록한다.
- 결제·환불 복구 배치의 마지막 성공 gauge는 애플리케이션 시작 시 0으로 미리 등록한다. 한 작업이 한 번도 실행되지 않은 경우에도 시계열이 빠지지 않아 정체 알림이 이를 감지한다.
- Prometheus는 최근 10분의 부분·전체 실패를 warning으로, 매분 실행되는 결제 준비 만료·결제 확정 복구·환불 복구 중 하나라도 마지막 정상 완료 뒤 5분을 넘기면 critical로 알린다.
- `OperationalBacklogMetrics`는 환불과 알림 outbox를 상태별 `COUNT`와 처리 기준 시각의 `MIN` 그룹 쿼리로 15초마다 스냅샷한다. scrape마다 DB를 조회하지 않으며, 조회 실패 때 마지막 정상 값을 유지하고 갱신 실패 횟수와 마지막 정상 갱신 후 경과 시간을 별도 지표로 노출한다.
- 승인 대기 주문과 예약 취소 후속 작업도 같은 스냅샷 주기로 건수와 가장 오래된 생성 기준 시각을
  집계한다. 한 번의 사건을 메모리 counter로 전달하지 않고 아직 처리되지 않은 DB 상태가 남은 동안
  경보를 유지한다. 가장 오래된 승인 대기 주문이 18시간을 넘으면 24시간 승인 마감까지 6시간 이하로
  남은 것으로 보고 critical 경보를 보낸다. business warning은 일반 warning의 4시간 주기와 분리해
  30분마다 재알림한다. 예약 취소 후속 작업의 `created_at`은 DB 기본값으로 생성되는 UTC 시각이므로
  oldest age 계산에서도 UTC로 해석한다.
- Prometheus는 결제 `RECONCILIATION_REQUIRED` DB backlog, 환불 `FAILED`·`RECONCILIATION_REQUIRED`, 상태별 처리 기준 시각을 5분 넘긴 자동 복구 backlog, 알림 outbox `FAILED`, 선점 후 2분 넘은 `PROCESSING`, 재시도 예정 시각을 1분 넘긴 `PENDING`, 1분 넘게 중단된 스냅샷 갱신을 각각 알린다. future `next_attempt_at`의 age는 0이므로 정상 백오프는 정체로 보지 않는다. Grafana system dashboard도 같은 상태별 건수·처리 기준 경과 시간·스냅샷 갱신 나이를 표시한다.
- HTTP 5xx 경보는 최근 5분 요청이 20건 이상이면 전체 요청 대비 10% 초과 비율을 사용하고, 저트래픽에서는 최근 15분 3건 이상을 별도 warning으로 감지한다.
- 결제와 알림 CircuitBreaker는 공용 `CircuitBreakerRegistry`에 등록하고 Resilience4j Micrometer tagged metrics를 노출한다. `/actuator/prometheus`와 Grafana에서 `name`·`state`·`kind`별 상태, 실패율, 호출 결과와 차단 호출을 확인한다. 서킷 자체의 최소 호출 수·실패율 판정 뒤 `OPEN` 상태 또는 최근 2분의 차단 호출을 즉시 평가해 결제는 critical, 알림 채널은 warning으로 알린다.
- 커밋 후 알림·환불 실행 신호 거절은 `happygallery.async.executor.rejected`로, 저장된 관리자 세션
  복호화·역직렬화 실패는 `happygallery.admin.session.validation.failures`로 계측한다. 전자는 DB 복구 경로를
  유지한 warning, 후자는 키·저장 데이터 이상을 뜻하는 critical 경보로 운영자가 확인한다.
- 비동기와 외부 호출 terminal boundary의 예상하지 못한 예외 로그에는 원 throwable을 포함하며,
  공용 `TaskDecorator`가 MDC requestId와 Sentry scope를 Spring 실행기와 raw 제한 큐에 함께 전달한다.
- 상태 변경형 대량 배치는 ID 오름차순 키셋을 사용한다. 실패한 항목도 현재 실행의 커서는 지나가고 다음 스케줄에서 재시도하므로, 같은 첫 페이지가 뒤 후보를 계속 가리는 문제를 막는다.
