# ADR-0030: 타임아웃 계층과 ingress keep-alive 기준선

**날짜**: 2026-03-29  
**상태**: Accepted

---

## 컨텍스트

운영 설정이 늘어나면서 프론트 요청 timeout, nginx `proxy_read_timeout`, 트랜잭션 timeout, DB query timeout, lock wait timeout, Hikari acquire timeout, ingress keep-alive가 여러 문서에 흩어졌다.

이 값들은 개별 숫자보다도 "바깥 계층이 더 길고 안쪽 계층이 더 짧다"는 순서와 "caller가 먼저 연결을 정리하고 callee가 더 오래 유지한다"는 원칙이 중요하다.

이 원칙이 문서에 명확히 남아 있지 않으면 특정 계층만 길어지거나 keep-alive 순서가 뒤집혀 장애 시 원인 추적이 어려워진다.

---

## 결정 사항

### 1. Actuator는 application 트래픽과 분리된 management port를 기본값으로 사용한다

- 기본 management port는 `8081` 이다.
- `local` 프로필에서는 `8080`으로 유지해 로컬 확인을 단순화한다.
- Actuator 웹 노출 정책은 `health`, `info`, `metrics`, `prometheus` 기준으로 유지한다.
- `prod`에서 `health details` 기본값은 `never`다.

### 2. timeout 기준선은 바깥 계층이 더 길고 안쪽 계층이 더 짧게 잡는다

- 기본 순서는 `frontend 35s > nginx read 30s > transaction 10s > DB query 5s > lock wait 3s > DB/Hikari acquire 2s` 이다.
- 프론트가 가장 늦게 포기하고, DB/Hikari가 가장 먼저 포기하도록 유지한다.
- 동기 MVC 전체 요청 deadline은 별도 필터 또는 컨테이너 커스터마이저 후보로 남겨 둔다.

### 3. ingress keep-alive는 caller가 먼저 닫고 callee가 더 오래 유지하는 방향을 우선한다

- ingress 기본값은 `client -> nginx keepalive_timeout 15s` 이다.
- `nginx -> app`은 upstream keep-alive를 활성화해 연결 재사용을 시작한다.
- idle 시간 세부값은 트래픽 특성을 보며 조정하되, caller가 더 먼저 연결을 정리하는 원칙은 유지한다.

### 4. 외부 HTTP 풀링 timeout도 전체 계층 안쪽에서 정렬한다

- 외부 알림/Google OAuth 호출은 `HTTP pool acquire 1s < connect 2s < read 5s` 순서를 따른다.
- downstream별 pool 분리와 max connections 기본값은 `ADR-0029`에서 관리한다.
- 외부 호출 timeout은 DB/Hikari보다 바깥에 있되, 프론트/nginx보다 안쪽에 있도록 유지한다.

---

## 결과

### 장점

- timeout 숫자와 keep-alive 설정을 개별 값이 아니라 계층 원칙으로 읽을 수 있다.
- 운영 중 특정 계층만 느려질 때 어디가 먼저 fail-fast 해야 하는지 기준이 분명해진다.
- ingress keep-alive 설정을 바꿀 때 stale connection 재사용 위험을 함께 검토할 수 있다.

### 단점

- 숫자는 트래픽과 외부 연동 특성에 따라 계속 조정해야 한다.
- nginx, 앱, DB, 프론트 설정이 함께 바뀌는 경우 문서와 설정을 같이 맞춰야 한다.

---

## 참고 문서

- `docs/ADR/0015_Observability_로깅과_비즈니스_예외/adr.md`
- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
- `app/src/main/resources/application.yml`
- `app/src/main/resources/application-local.yml`
- `README.md`
- `HANDOFF.md`
