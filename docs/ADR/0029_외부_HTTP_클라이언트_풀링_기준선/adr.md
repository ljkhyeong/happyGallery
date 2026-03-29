# ADR-0029: 외부 HTTP 클라이언트 풀링 기준선

**날짜**: 2026-03-29  
**상태**: Accepted

---

## 컨텍스트

`prod` 프로필의 외부 알림(Kakao 알림톡, NHN SMS)과 Google OAuth 호출은 Spring `RestClient`를 사용하지만, 기존에는 `SimpleClientHttpRequestFactory` 기반이라 downstream별 connection pool이 없었다.

이 구조는 호출량이 늘거나 외부 응답이 느릴 때 다음 문제가 생긴다.

- TCP/TLS 연결 재사용 이점이 없다.
- 한 외부 서비스의 연결 대기가 다른 호출과 구분되지 않는다.
- connection acquire timeout, max connections 같은 운영 기준선을 명시하기 어렵다.

---

## 결정 사항

### 1. `prod` 프로필의 외부 HTTP 호출은 Apache HttpClient 5 기반 pool을 사용한다

- `RestClient`는 유지한다.
- request factory만 `HttpComponentsClientHttpRequestFactory`로 바꾼다.
- pool은 downstream별로 분리한다.
  - Kakao 알림톡
  - NHN SMS
  - Google OAuth

### 2. timeout과 풀 크기는 downstream별 프로퍼티로 관리한다

기본값:

- `acquire timeout`: 1초
- `connect timeout`: 2초
- `read timeout`: 5초
- `keep-alive`: 30초
- 알림(Kakao, SMS) max connections: 20
- Google OAuth max connections: 10

환경 변수 예시:

- `KAKAO_TIMEOUT_MILLIS`
- `KAKAO_ACQUIRE_TIMEOUT_MILLIS`
- `KAKAO_CONNECT_TIMEOUT_MILLIS`
- `KAKAO_MAX_CONNECTIONS`
- `KAKAO_KEEP_ALIVE_MILLIS`
- `SMS_TIMEOUT_MILLIS`
- `SMS_ACQUIRE_TIMEOUT_MILLIS`
- `SMS_CONNECT_TIMEOUT_MILLIS`
- `SMS_MAX_CONNECTIONS`
- `SMS_KEEP_ALIVE_MILLIS`
- `GOOGLE_OAUTH_TIMEOUT_MILLIS`
- `GOOGLE_OAUTH_ACQUIRE_TIMEOUT_MILLIS`
- `GOOGLE_OAUTH_CONNECT_TIMEOUT_MILLIS`
- `GOOGLE_OAUTH_MAX_CONNECTIONS`
- `GOOGLE_OAUTH_KEEP_ALIVE_MILLIS`

### 3. 풀링 기준선도 전체 timeout 계층 안쪽에 둔다

- `frontend 35s > nginx read 30s > transaction 10s > DB query 5s > lock wait 3s > DB/Hikari acquire 2s`
- 외부 HTTP는 이와 별도로 `HTTP pool acquire 1s < connect 2s < read 5s`를 따른다.
- 동기 MVC 전체 요청 deadline은 별도 필터/컨테이너 커스터마이저 후보로 남긴다.

---

## 결과

### 장점

- 외부 알림/OAuth 호출의 연결 재사용과 동시성 제한이 가능해진다.
- 서비스별 장애 격리와 운영 튜닝 포인트가 더 분명해진다.
- acquire timeout을 통해 pool 고갈을 호출 처리 초반에 빠르게 드러낼 수 있다.

### 단점

- 설정 항목과 운영 튜닝 포인트가 늘어난다.
- downstream 특성에 따라 max connections와 keep-alive 값을 추가 조정해야 할 수 있다.

---

## 참고 문서

- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0023_관리자_인증과_런타임_운영_기준선/adr.md`
- `README.md`
