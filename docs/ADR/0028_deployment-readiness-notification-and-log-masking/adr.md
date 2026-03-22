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
- infra 모듈에 `spring-web` 의존성을 추가하지 않기 위해 `java.net.http.HttpClient`를 사용.
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
- `logstash-logback-encoder`를 `runtimeOnly` → `implementation`으로 변경 (커스텀 JsonProvider 컴파일에 필요).

### 5. 배포 인프라

- `nginx/nginx.conf`: SPA fallback (`try_files $uri /index.html`) + API 리버스 프록시 (`proxy_pass http://app:8080`).
- `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` 헤더 전달.
- `docker-compose.yml`에 `nginx` 서비스 추가 (frontend `dist` 마운트).
- `application-prod.yml`:
  - `server.forward-headers-strategy: native` — Tomcat `RemoteIpValve` 활성화.
  - `app.rate-limit.trust-forwarded-headers: true` — 프록시 뒤 실제 IP 기반 rate limiting.
- Grafana 인증 환경변수 외부화: `${GRAFANA_ADMIN_PASSWORD}` (기본값 없음).
- `.env.example` 통합 — Kakao, SMS, Sentry, Grafana 환경변수 추가.

## 1차 배포 제외 항목 (Known Gaps)

- **번들 결제** (PRD §6): 스키마 준비 완료(`bundle_id nullable`), 구현은 Phase 2.
- **Email/Push 알림 채널** (PRD §7): `NotificationChannel` enum에 값 존재, 어댑터 미구현.
- **PG 실 연동**: `FakePaymentProvider` 유지. CircuitBreaker/timeout은 프로덕션 준비 완료.
- **Tomcat internal-proxies**: Nginx 앞에 ALB 등 외부 프록시가 추가되면 설정 필요 (`docs/Idea/0027`).

## 참고

- PRD §3.3 (픽업 규칙), PRD §7 (알림 정책)
- ADR-0015 (로그 구조화), ADR-0017 (rate limiting), ADR-0025 (graceful shutdown)
