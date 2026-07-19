# Spring Security 전면 전환 검토

> **상태**: 채택·구현 완료
> 현재 인증·세션 기준은 `docs/ADR/0023_관리자_회원_인증_세션_기준선/adr.md`, HTTP 계약은 `docs/PRD/0004_API_계약/spec.md`를 따른다.

---

## 채택한 범위

- Spring Boot 4.0.2 기준 `spring-boot-starter-security`를 사용한다.
- 관리자 요청과 회원·공개 요청을 두 개의 `SecurityFilterChain`으로 분리한다.
- 관리자 Redis Bearer 세션, local 전용 API key, 회원 `HG_SESSION`과 `customerUserId` 저장 계약은 유지한다.
- 관리자·회원 인증은 요청마다 principal과 `SecurityContext`로 표현한다.
- 컨트롤러는 `@AuthenticationPrincipal`로 typed principal을 직접 받고 필요한 식별자를 유스케이스에 전달한다. 인증이 선택인 API는 nullable `CustomerPrincipal`로 회원과 게스트를 구분하고, `/api/v1/me`는 principal의 회원 스냅샷을 재사용한다.
- `RequestIdFilter`, `RateLimitFilter`는 Security 체인 앞단에 유지한다.
- 회원 로그인 성공 시 세션 ID를 회전한다.
- 인증·인가 실패는 기존 `ErrorResponse` 형태의 JSON과 `401`·`403`으로 반환한다.
- 회원·공개 체인은 SPA CSRF 보호를 적용하고, 관리자 Bearer/API key 체인은 CSRF 대상에서 제외한다.
- Spring Security 기본 cache-control writer는 비활성화해 기존 ETag와 API별 캐시 정책을 보존한다.
- Google·Naver 로그인 프로토콜은 `spring-boot-starter-oauth2-client`가 처리한다. application에는 검증된 소셜 프로필만 전달하고 장기 인증 상태는 기존 `customerUserId` 세션 하나로 유지한다.

## 이번 전환에서 제외한 범위

- 관리자 인증은 자체 Redis 세션이므로 JWT와 OAuth2 Resource Server를 도입하지 않는다.
- 현재 역할 모델은 관리자·회원 구분이면 충분하므로 method security를 추가하지 않는다.
- 운영·로컬 모두 same-origin 구성이므로 별도 CORS 정책을 추가하지 않는다.

---

## 현재 기준 문서

- 인증·세션 설계: `docs/ADR/0023_관리자_회원_인증_세션_기준선/adr.md`
- HTTP·CSRF 계약: `docs/PRD/0004_API_계약/spec.md`
