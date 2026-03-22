# Idea 0024: HttpClient → RestClient 전환 검토

## 배경

`infra/notification/` 패키지의 외부 API 호출(`KakaoAlimtalkSender`, `RealSmsSender`)은 `java.net.http.HttpClient`를 사용 중이다.
JDK 표준 라이브러리라 Spring 의존이 없다는 장점은 있다. 하지만 `infra` 모듈은 이미 `@Component`, `@Profile` 등 Spring에 전면 의존하고 있어 JDK 표준만 고집할 실익은 작다.

## 현재 문제

- JSON 요청 바디를 text block으로 수동 조립 → 오타·이스케이프 누락 위험.
- 응답 상태 코드 200-299 범위 체크를 매번 직접 작성.
- `HttpClient` 인스턴스를 생성자에서 직접 빌드 → timeout 설정이 각 Sender에 흩어짐.
- 외부 API 호출 단위 테스트 시 `HttpClient` 목킹이 까다로움 (현재는 `NotificationSender` 인터페이스 수준에서만 테스트).

## 제안: Spring `RestClient` 전환

Spring 6.1+ (Boot 3.2+)에서 도입된 `RestClient`는 `RestTemplate`의 fluent 후속작이다.

### 기대 효과

| 항목 | HttpClient (현재) | RestClient (전환 후) |
|------|-------------------|---------------------|
| JSON 직렬화 | 수동 text block | `.body(dto)` → Jackson 자동 |
| 에러 핸들링 | `response.statusCode()` 수동 체크 | `.onStatus()` 선언적 핸들러 |
| 응답 역직렬화 | `BodyHandlers.ofString()` + 수동 파싱 | `.retrieve().body(Dto.class)` |
| timeout 관리 | 각 Sender 생성자에서 개별 설정 | `ClientHttpRequestFactory` 중앙 관리 |
| 테스트 | 인터페이스 수준 목킹만 가능 | `MockRestServiceServer` 사용 가능 |
| Spring 통합 | 별도 | interceptor, message converter 공유 |

### 전환 범위

- `KakaoAlimtalkSender` — `HttpClient` → `RestClient`
- `RealSmsSender` — `HttpClient` → `RestClient`
- 공통 `RestClient` 빈을 `NotificationConfig` 등에서 빌드하고 각 Sender에 주입하는 구조 고려.

### 주의사항

- `WebClient`(리액티브)는 프로젝트가 동기 서블릿 기반이므로 과하다. `RestClient`가 더 맞다.
- payment 영역의 `CircuitBreakerPaymentProvider`는 이미 별도 resilience 레이어가 있으므로 notification과는 독립적으로 판단.
- 전환 시 기존 대체 발송 순서(KAKAO→SMS)와 `NotificationSender` 인터페이스 계약은 바꾸지 않는다.

## 상태

검토 중이다. 구현 우선순위는 아직 정하지 않았다.
