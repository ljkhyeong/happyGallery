# ADR-0029: 외부 HTTP 클라이언트 풀 설정

**날짜**: 2026-03-29  
**최종 갱신**: 2026-08-27
**상태**: Accepted

---

## 왜 이 문서가 필요한가

알림 발송과 Google, Naver, Kakao OAuth 호출은 모두 외부 HTTP 의존성이 있다.
연결 풀 없이 호출하면 느린 외부 서비스 하나가 다른 호출까지 쉽게 끌어내린다.
Toss Payments confirm/cancel, Delivery API 운송장 등록, 도로명주소 검색과 공휴일 조회도 같은 외부 HTTP 경계에 포함된다.

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
  - Kakao OAuth
  - Toss Payments
  - Delivery API 배송조회
  - 주소기반산업지원서비스 도로명주소
  - 공공데이터포털 한국천문연구원 특일 정보

### 2. 타임아웃과 풀 크기는 서비스별 프로퍼티로 관리한다

현재 기본값:

- Alimtalk/SMS: acquire 0.5초, connect 1초, read/response 2초
- 알림 바깥 TimeLimiter: 5초
- OAuth: acquire 1초, connect 2초, read/response 5초
- Toss: acquire 0.5초, connect 1초, read/response 3초, 바깥 TimeLimiter 5초
- keep-alive: 30초
- 알림(Alimtalk, SMS) max connections: 20
- Google/Naver/Kakao OAuth provider별 max connections: 10
- Toss Payments max connections: 10
- Delivery API: acquire 0.5초, connect 1초, read/response 3초, max connections 10
- 도로명주소: acquire 0.5초, connect 1초, read/response 3초, max connections 10
- 공휴일: acquire 0.5초, connect 1초, read/response 5초, max connections 5
- acquire·connect·response·keep-alive는 각 `@ConfigurationProperties`에서 `Duration`으로 바인딩한다. `PooledHttpClientFactory`는 임의의 밀리초 변환 없이 Apache HttpClient 5의 `Timeout.of(Duration)`와 `TimeValue.of(Duration)`에 전달한다.
- 기존 `*_TIMEOUT_MILLIS`·`*_KEEP_ALIVE_MILLIS` 환경 변수는 숫자 계약을 유지하고 `application.yml`에서 `ms` 단위를 붙인다.

### 3. 외부 HTTP 설정도 전체 타임아웃 계층 안에서 정렬한다

- Alimtalk/SMS는 `acquire + connect + response < TimeLimiter` 순서를 지킨다. 기본값은 `0.5s + 1s + 2s < 5s`다.
- Toss도 같은 순서로 `0.5s + 1s + 3s < 5s`를 지킨다.
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
- 시간 단위를 설정 이름과 산술에 중복 표현하지 않아 단위 변환 오류와 설정 관리 지점이 줄어든다.

### 단점

- 설정 항목이 늘어난다.
- 서비스 특성에 따라 연결 수와 keep-alive를 추가 조정해야 할 수 있다.

---

## 참고 문서

- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0030_타임아웃_계층과_ingress_keep_alive_기준선/adr.md`
- `README.md`
