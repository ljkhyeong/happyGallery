# ADR-0029: 외부 HTTP 클라이언트 풀 설정

**날짜**: 2026-03-29  
**최종 갱신**: 2026-07-27
**상태**: Accepted

---

## 왜 이 문서가 필요한가

알림 발송과 Google, Naver OAuth 호출은 모두 외부 HTTP 의존성이 있다.
연결 풀 없이 호출하면 느린 외부 서비스 하나가 다른 호출까지 쉽게 끌어내린다.
Toss Payments confirm/cancel 호출도 같은 외부 HTTP 경계에 포함된다.

---

## 결정

### 1. `prod` 프로필의 외부 HTTP 호출은 Apache HttpClient 5 기반 연결 풀을 사용한다

- 일반 외부 API는 `RestClient`, OAuth token 교환은 Spring Security의 `RestClientAuthorizationCodeTokenResponseClient`, OAuth UserInfo는 `DefaultOAuth2UserService`를 사용한다.
- 각 클라이언트의 request factory를 `HttpComponentsClientHttpRequestFactory`로 바꾼다.
- `RestClient`와 `RestTemplate`은 각각 Spring Boot가 자동 구성한 prototype `RestClient.Builder`와
  `RestTemplateBuilder`에서 생성한다. 서비스별 request factory, 인증 헤더, 메시지 converter와 오류
  handler는 builder에 추가하되 Boot의 HTTP message converter, customizer와 `http.client.requests`
  observation을 우회하지 않는다.
- 풀은 서비스별로 분리한다.
  - NHN Cloud Alimtalk
  - NHN SMS
  - Google OAuth
  - Naver OAuth
  - Toss Payments

### 2. 타임아웃과 풀 크기는 서비스별 프로퍼티로 관리한다

현재 기본값:

- Alimtalk/SMS: acquire 0.5초, connect 1초, read/response 2초
- 알림 바깥 TimeLimiter: 5초
- OAuth/Toss: acquire 1초, connect 2초, read/response 5초
- keep-alive: 30초
- 알림(Alimtalk, SMS) max connections: 20
- Google/Naver OAuth provider별 max connections: 10
- Toss Payments max connections: 10

### 3. 외부 HTTP 설정도 전체 타임아웃 계층 안에서 정렬한다

- Alimtalk/SMS는 `acquire + connect + response < TimeLimiter` 순서를 지킨다. 기본값은 `0.5s + 1s + 2s < 5s`다.
- 운영 환경변수로 이 순서가 역전되면 애플리케이션 기동을 거부한다.
- 이 값은 프론트와 ingress 타임아웃보다 안쪽에 둔다.
- 전체 타임아웃 원칙은 `ADR-0030`을 따른다.

---

## 결과

### 장점

- 연결 재사용과 동시성 제한이 가능해진다.
- 한 서비스의 지연이 다른 서비스로 번지는 범위를 줄일 수 있다.
- 운영 튜닝 포인트가 분명해진다.
- 외부 호출의 공통 Micrometer 관측성과 Boot HTTP client customization이 서비스별 풀에도 유지된다.

### 단점

- 설정 항목이 늘어난다.
- 서비스 특성에 따라 연결 수와 keep-alive를 추가 조정해야 할 수 있다.

---

## 참고 문서

- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0030_타임아웃_계층과_ingress_keep_alive_기준선/adr.md`
- `README.md`
