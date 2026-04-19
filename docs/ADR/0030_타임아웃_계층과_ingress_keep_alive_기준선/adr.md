# ADR-0030: 타임아웃 계층과 ingress keep-alive 운영 기준

**날짜**: 2026-03-29  
**상태**: Accepted

---

## 왜 이 문서가 필요한가

프론트 요청 타임아웃, ingress 프록시 타임아웃, 트랜잭션 타임아웃, DB 대기 시간은 숫자만 맞추면 끝나는 문제가 아니다.  
각 계층이 어떤 순서로 먼저 포기하고, 어떤 연결을 더 오래 유지할지 원칙이 분명해야 운영 중 원인 추적이 쉬워진다.

---

## 결정

### 1. Actuator는 application 트래픽과 분리된 management port를 기본으로 둔다

- 기본 management port는 `8081`
- `local` 프로필에서는 `8080`
- 웹 노출은 `health`, `info`, `metrics`, `prometheus`
- `prod`의 `health details` 기본값은 `never`

### 2. 타임아웃은 바깥 계층이 더 길고 안쪽 계층이 더 짧아야 한다

현재 기본 순서:

- frontend: 35초
- ingress `proxy_read_timeout`: 30초
- transaction timeout: 10초
- JPA query timeout: 5초
- MySQL lock wait: 3초
- DB/Hikari acquire: 2초

즉, 프론트가 가장 늦게 포기하고 DB와 풀 획득이 가장 먼저 포기한다.

### 3. keep-alive는 호출하는 쪽이 먼저 정리하고, 받는 쪽이 더 오래 유지한다

- `client -> ingress`: keepalive 15초
- `ingress -> app`: upstream keep-alive 활성화

세부 숫자는 조정할 수 있지만, 호출하는 쪽이 먼저 연결을 정리한다는 원칙은 유지한다.

### 4. 외부 HTTP 호출도 같은 계층 원칙 안에 둔다

- 외부 HTTP는 `pool acquire 1s < connect 2s < read 5s`
- 서비스별 연결 풀과 세부 설정은 `ADR-0029`에서 관리한다.

---

## 결과

### 장점

- 타임아웃 숫자를 개별 상수가 아니라 계층 원칙으로 볼 수 있다.
- 장애 시 어느 계층이 먼저 fail-fast 해야 하는지 기준이 분명하다.

### 단점

- 트래픽과 외부 연동 특성에 따라 숫자는 계속 조정해야 한다.
- 프론트, ingress, 앱, DB 설정을 함께 맞춰야 한다.

---

## 참고 문서

- `docs/ADR/0015_Observability_로깅과_비즈니스_예외/adr.md`
- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
- `bootstrap/src/main/resources/application.yml`
- `bootstrap/src/main/resources/application-local.yml`
- `README.md`
- `HANDOFF.md`
