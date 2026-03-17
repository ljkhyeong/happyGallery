# ConfigurationProperties 기반 설정 바인딩 정리 메모

**날짜**: 2026-03-17  
**상태**: Working note (이미 일부 적용, 이후 확장 기준)

---

## 배경

설정 항목이 늘어날수록 개별 `@Value` 주입이나 문자열 key 직접 참조는 기본값, 검증, 문서화가 쉽게 흩어진다.

현재 코드베이스는 주요 운영 설정을 `@ConfigurationProperties`로 묶는 방향으로 이미 전환했고, 이 문서는 그 방향을 계속 유지하기 위한 기준을 남긴다.

---

## 현재 적용 상태

| 영역 | prefix | 클래스 | 용도 |
|------|--------|---------|------|
| 관리자 인증 | `app.admin` | `AdminProperties` | API key, API key 인증 허용 여부 |
| Rate limit | `app.rate-limit` | `RateLimitProperties` | 공개/회원/관리자 경로 제한값과 forwarded header 신뢰 여부 |
| 배치 스케줄러 | `app.batch.scheduler` | `BatchSchedulerProperties` | 스케줄러 thread pool 크기 |

코드 검색 기준으로 현재 `app/src/main/java`에는 `@Value` 직접 주입이 남아 있지 않다.

---

## 유지 원칙

1. 새 `app.*` 설정은 가능한 한 concern 단위의 `@ConfigurationProperties` 클래스로 묶는다.
2. 기본값은 properties 클래스 필드에 둔다.
3. 숫자 범위나 필수 조건은 `@Validated`와 Bean Validation으로 검증한다.
4. filter/service에는 개별 primitive보다 properties 객체를 주입한다.
5. 비밀값은 저장소에 하드코딩하지 않고 환경 변수로 주입한다.
6. 외부 계약에 가까운 설정 키를 바꾸면 README/ADR/운영 문서도 같이 맞춘다.

---

## 이미 얻은 효과

- 관리자 인증 기본값과 rate limit 기본값을 한 곳에서 읽을 수 있다.
- 기본값과 검증 규칙이 설정 클래스에 모여 회귀 테스트 작성이 쉬워졌다.
- Filter/설정 생성 코드가 문자열 key보다 타입 중심으로 정리됐다.

---

## 앞으로의 적용 기준

- 새 운영 설정군이 생기면 먼저 `app.<domain>` prefix와 properties 클래스를 정의한다.
- 설정 수가 1~2개뿐이더라도 재사용되거나 검증이 필요하면 `@ConfigurationProperties`를 우선 검토한다.
- 이미 안정적인 Spring/infra 기본 설정은 무리해서 래핑하지 않고, `app.*` 아래의 애플리케이션 설정부터 일관성을 맞춘다.

---

## 현재 결론

`ConfigurationProperties` 전환은 이미 일부 완료된 리팩토링이지만, 앞으로 새 설정을 추가할 때 계속 지켜야 할 코드베이스 규칙으로 이 문서에서 관리한다.
