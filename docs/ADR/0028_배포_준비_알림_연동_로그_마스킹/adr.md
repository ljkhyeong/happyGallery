# ADR-0028: 1차 배포 준비 — 알림 실 연동, 로그 마스킹, 배포 인프라

## 상태

채택 (2026-03-22)

## 배경

1차 프로덕션 배포를 앞두고 다음 영역에서 보강이 필요했다.

1. **알림 어댑터**: `FakeKakaoSender`/`FakeSmsSender`만 존재해 프로덕션에서 알림이 발송되지 않음
2. **픽업 마감 알림 누락**: PRD §3.3의 "매장 마감 2시간 전 알림" 배치가 미구현
3. **세션 쿠키 Secure 플래그 부재**: HTTPS 리버스 프록시 뒤에서 쿠키가 평문 HTTP로 전송될 위험
4. **로그 민감 데이터 노출**: 전화번호, Bearer 토큰, 세션 토큰이 로그에 평문으로 기록
5. **배포 인프라 미비**: Nginx 리버스 프록시, forwarded headers 설정, 환경변수 정리 필요

## 결정

### 1. 알림 어댑터 프로필 분리

- 기존 `FakeKakaoSender`/`FakeSmsSender`에 `@Profile("!prod")`를 추가해 비운영 전용으로 격리.
- `KakaoAlimtalkSender`(`@Order(1)`, `@Profile("prod")`)와 `RealSmsSender`(`@Order(2)`, `@Profile("prod")`)를 신규 추가.
- 기존 `NotificationSenderPort` + `@Order` fallback 체인을 그대로 활용해 서비스 계층 변경 없음.
- `RestClient` + Apache HttpClient 5 연결 풀을 사용한다. 서비스별 풀 기준은 `ADR-0029`를 따른다.
- 외부 설정: `app.external.kakao.*`, `app.external.sms.*` (`application.yml`에 환경변수 바인딩).
- `KakaoNotificationProperties`, `SmsNotificationProperties`를 `@ConfigurationProperties` record로 정의.

### 2. 픽업 마감 2시간 전 알림 배치

- `NotificationEventType.PICKUP_DEADLINE_REMINDER` 추가.
- `FulfillmentPort.findPickupsApproachingDeadline(from, to)`: `PICKUP_READY` 상태이고 `pickupDeadlineAt`이 `now~now+2h` 범위인 fulfillment를 조회.
- `DefaultPickupDeadlineReminderBatchService`: 주문의 guest/user 분기 발송, `NotificationLogReaderPort`로 24시간 내 중복 방지.
- `BatchScheduler`에 매시간 정각 cron 등록.

### 3. 세션 쿠키 Secure 플래그

- `RedisConfig.cookieSerializer()`에 `setUseSecureCookie(true)` 추가.
- 프로필 분기 없이 일괄 적용 — 최신 브라우저는 localhost를 secure context로 취급하므로 로컬 개발에 영향 없음.

### 4. 로그 민감 데이터 마스킹

- `MaskingPatternLayout` (extends `PatternLayout`): text 프로필(`!prod`)에서 `LayoutWrappingEncoder`로 사용.
- `MaskingMessageJsonProvider` (extends `AbstractFieldJsonProvider`): prod JSON 프로필에서 `LoggingEventCompositeJsonEncoder`의 message provider로 사용.
- 양쪽 모두 `MaskingPatternLayout.maskSensitive()` 정적 메서드를 공유해 마스킹 로직을 단일화.
- 마스킹 대상:
  - 전화번호: `01x-xxxx-xxxx` → `01x-****-****`
  - Bearer 토큰: `Bearer xxx` → `Bearer ***`
  - 세션 토큰: `HG_SESSION=xxx` → `HG_SESSION=***`
  - Access 토큰: `X-Access-Token=xxx` → `X-Access-Token=***`
- 마스킹은 예기치 않은 문자열 유입을 막는 방어선으로 유지하되, 애플리케이션 로그 호출 자체에도 전화번호·이름·인증 코드·결제 키를 전달하지 않는다.
- 알림·결제 외부 호출 실패는 예외 원문 대신 HTTP 상태, 예외 타입과 내부 식별자만 기록한다. `notification_log.fail_reason` 등 영속 실패 사유에는 `DELIVERY_EXCEPTION` 같은 통제된 문구를 저장한다.
- 전역 예외 처리와 Sentry 전송은 DB·JSON·외부 서비스 예외 원문 대신 예외 종류와 공통 오류 메시지만 남긴다.
- 로컬 Hibernate SQL bind 로깅도 개인정보가 노출되지 않도록 `WARN` 수준으로 유지한다.
- `logstash-logback-encoder`를 `runtimeOnly` → `implementation`으로 변경 (커스텀 JsonProvider 컴파일에 필요).

### 5. 배포 인프라

- `nginx/nginx.conf`와 `docker-compose.yml`의 Nginx 서비스는 SPA fallback과 API 프록시를 포함한 로컬 통합 검증·복구 진단용으로 유지한다.
- 운영 목표는 ADR-0037에 따라 단일 노트북의 단일 노드 k3s와 Kubernetes Ingress로 전환한다. Docker Compose의 `local` 프로필과 개발 기본값을 운영 구성으로 사용하지 않는다.
- ingress는 `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`를 덮어쓰거나 정규화하고 애플리케이션 직접 접근을 차단한다.
- `application-prod.yml`은 forwarded header 신뢰를 기본적으로 끈다. 통제된 ingress가 헤더를 덮어쓰고 직접 접근을 차단한 뒤에만 `FORWARD_HEADERS_STRATEGY=native`, `RATE_LIMIT_TRUST_FORWARDED=true`를 함께 설정한다.
- Grafana 인증 환경변수 외부화와 `.env.example`의 로컬 설정 목록은 유지하되, 운영 secret은 Kubernetes 실행 환경에서 저장소 밖의 값으로 주입한다.
- AWS 자동 배포는 폐기한다. k3s manifest와 배포·rollback 절차가 구현되기 전까지 운영 배포는 미구현 상태다.

## 1차 배포 제외 항목 (Known Gaps)

- **번들 결제** (PRD §6): 스키마 준비 완료(`bundle_id nullable`), 구현은 Phase 2.
- **Email/Push 알림 채널** (PRD §7): `NotificationChannel` enum에 값 존재, 어댑터 미구현.
- **k3s 운영 배포**: Kubernetes manifest, ingress/TLS, 영속 볼륨, secret 주입, 백업·복원, 이미지 rollout·rollback 절차가 아직 없다. 완료 전에는 공개 운영 배포로 간주하지 않는다(ADR-0037).
- **신뢰 프록시 경계**: 공유기, 터널 또는 별도 프록시를 ingress 앞에 추가하면 신뢰 가능한 홉과 전달 헤더 정규화 규칙을 명시하고 실제 IP 기반 처리율 제한을 다시 검증해야 한다.

## Update (2026-04-26)

- PG 실 연동 제외 항목은 해소됐다. 운영(`prod`)은 `TossPaymentsProvider`, 비운영(`!prod`)은 `FakePaymentProvider`를 사용한다.
- Toss confirm/cancel도 `ResilientPaymentProvider`와 전용 pooled `RestClient` 경계를 통과한다.

## Update (2026-07-17)

- 휴대폰 인증 발급 로그에서 전화번호와 인증 코드를 제거했다.
- 알림 sender와 Toss 결제·환불 로그에서 수신자 정보, 결제 키와 외부 예외 원문을 제거했다.
- 저장 개인정보와 Redis 키 보호 기준은 ADR-0036으로 분리한다.

## Update (2026-07-18)

- AWS 배포 기준을 폐기하고 단일 노트북 k3s를 운영 목표로 채택했다.
- 이 ADR의 Nginx·Docker Compose 구성은 로컬 통합 검증과 복구 진단 범위로 한정하며, 운영 토폴로지와 미구현 항목은 ADR-0037을 따른다.

## 참고

- PRD §3.3 (픽업 규칙), PRD §7 (알림 정책)
- ADR-0015 (로그 구조화), ADR-0017 (rate limiting), ADR-0025 (graceful shutdown)
- ADR-0036 (개인정보 평문 제거와 블라인드 인덱스)
- ADR-0037 (자가 호스팅 배포 토폴로지 기준)
