# ADR-0017: 필터 기반 API 처리율 제한 도입

**날짜**: 2026-03-06  
**상태**: Accepted

---

## 컨텍스트

무제한 API 호출이 가능하면 특정 IP/봇 트래픽이 애플리케이션/DB 자원을 고갈시켜
정상 사용자 요청까지 장애로 전파될 수 있다.

특히 본 프로젝트의 인증코드 발송, 게스트 예약 생성, 이용권 구매, 관리자 운영 API는
짧은 시간 내 대량 호출될 가능성이 있어 선제적인 요청 제한이 필요하다.

---

## 결정 사항

### 1. Interceptor가 아닌 Servlet Filter 계층에서 제한한다

- 요청 초기 단계에서 차단해 컨트롤러/서비스/DB 진입 전에 자원을 보호한다.
- 필터 순서:
  - `RequestIdFilter` → `RateLimitFilter` → `AdminAuthFilter`

### 2. `Bucket4j` 기반 토큰 버킷 정책을 적용한다

- 키: 기본적으로 클라이언트 IP(`remoteAddr`)
- `app.rate-limit.trust-forwarded-headers=true` 일 때만 `X-Forwarded-For`의 첫 번째 IP를 신뢰한다
- 기본 한도:
  - 인증코드 발송: 10 req/sec/IP
  - 게스트 예약 생성: 30 req/min/IP
  - 이용권 구매: 20 req/min/IP
  - 관리자 로그인: 5 req/min/IP
  - Admin API: 120 req/min/IP

### 3. 메모리 저장소는 bounded cleanup을 둔다

- 인메모리 bucket map은 마지막 접근 시각을 기록한다
- 5분마다 10분 이상 미접근 bucket을 제거해 장시간 실행 시 무제한 증가를 줄인다

### 4. 제한 초과 시 표준 에러 응답을 반환한다

- HTTP `429 TOO_MANY_REQUESTS`
- 본문: `{"code":"TOO_MANY_REQUESTS","message":"..."}`
- 헤더: `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 대량 호출을 조기에 차단해 서버/DB 보호 효과가 크다 |
| 장점 | 운영자가 `429` 지표로 비정상 트래픽을 빠르게 탐지 가능 |
| 단점 | 과도하게 낮은 제한값은 정상 사용자 UX에 영향을 줄 수 있음 |
| 대응 | 제한값을 `application.yml`로 외부화해 환경별 튜닝 |

---

## 구현 반영

- `app/web/RateLimitFilter` 추가
- `app/web/RequestIdFilter`, `app/web/AdminAuthFilter` 필터 순서/경로 보강
- `common/error/ErrorCode`에 `TOO_MANY_REQUESTS` 추가
- `application.yml`에 `app.rate-limit.*` 설정 추가
- `ADMIN_LOGIN` 한도와 `trust-forwarded-headers` 정책 추가
