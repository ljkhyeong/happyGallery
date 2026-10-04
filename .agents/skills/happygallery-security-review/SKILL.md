---
name: happygallery-security-review
description: 사용자가 happyGallery의 보안 리뷰·위협 모델·보안 점검을 요청할 때 사용한다. 일반 결함 리뷰는 happygallery-code-review, 보안 기능의 구현·수정은 해당 도메인 스킬을 사용한다.
---

# happyGallery 보안 검토

## 범위

- diff 리뷰의 대상 확정·읽기 전용·심각도(P0~P3) 형식은 `happygallery-code-review`를 따르고, 이 스킬은 보안 관점만 더한다.
- 규칙 원본과 대조하고 복제하지 않는다. 관리자 인증 `happygallery-admin-flows`, 로그인·OAuth·휴대폰 `happygallery-identity-flows`, `/me`·비회원 기록 연결 `happygallery-member-flows`, 결제 `happygallery-payment-flows`, 쿠폰·적립금 `happygallery-benefit-flows`, 후기·이미지 미디어 `happygallery-review-flows`, 스마트스토어 `happygallery-smartstore-flows`, 로그·Sentry `happygallery-observability-flows`, Ingress·Secret `happygallery-deploy-ops`, 화면·token 저장 `happygallery-frontend-flows`, 공개 SSR loader·JSON-LD `happygallery-seo-ssr`.
- 범용 체크리스트는 아래 경계·자산과 연결될 때만 보고한다. ADR로 정한 선택(계정 잠금 없음, CORS 없음, CSP Report-Only)은 결함이 아니라 전제로 다룬다. 운영 Secure cookie·API key 차단은 `ProductionRuntimeGuard`가 강제하므로 local 설정만 보고 지적하지 않는다.

## 신뢰 경계와 자산

- 인터넷 → Traefik Ingress(TLS) → `/api/*`는 app 8080, 나머지는 Node SSR이다. Actuator 8081·MySQL·Redis·Grafana는 cluster 내부다(ADR-0037).
- 관리자는 Redis Bearer 세션과 prod MFA, 회원은 `HG_SESSION`·SPA CSRF·요청별 `credential_version` 비교로 인증한다(ADR-0023).
- 비회원은 HMAC 서명·만료 token을 `X-Access-Token`·`X-Payment-Status-Token` 헤더로 보내고 DB에는 해시만 둔다(ADR-0024).
- 외부 입력: OAuth callback, Toss·배송 웹훅, 후기 이미지 업로드, 공개 이미지 조회, 공개 SSR loader, 클라이언트 모니터링 수집.
- 자산: AES-GCM 암호문·HMAC 인덱스로 보호한 PII·배송지, 비회원 token, 결제 금액·paymentKey, 관리자 TOTP·복구 코드, `ENCRYPT_KEY`·`HMAC_KEY`·guest token 서명 키(ADR-0036).

## 리뷰 관점

- 인가: 회원·공개 체인은 `SecurityConfig`의 메서드별 허용 목록 밖을 `denyAll`, 관리자 체인은 `ROLE_ADMIN`으로 막는다. permitAll 추가와 회원·결제·OAuth·처리율 경로의 `CustomerSecurityRoutes` 사용을 본다.
- 소유권: 회원은 principal의 `userId`, 비회원은 `GuestTokenService.resolveTokenHash` 결과를 조회 조건에 넣고 불일치는 404로 숨긴다. ID만으로 읽거나 바꾸는 경로를 찾는다.
- CSRF·캐시: 쿠키 체인의 CSRF 예외는 배송 웹훅(HMAC)·Toss 웹훅(PG 재조회)뿐이고 관리자 체인은 헤더 인증이라 CSRF가 없다. 예외 추가, 관리자 쿠키 인증 도입, 민감 응답의 `no-store` matcher 누락을 본다.
- 처리율: 인증 코드·로그인·결제 prepare·공개 접수처럼 비용이 큰 경로는 `RateLimitFilter` 전용 fail-closed 규칙이 필요하다. 마지막 `DEFAULT_API_IP`는 fail-open이고 client key는 `getRemoteAddr()`만 쓴다(ADR-0017).
- 비회원 token: URL·query·로그·Sentry로 새지 않는지, `resolveTokenHash`를 거치지 않은 비교나 만료 우회가 없는지, 프론트 저장이 세대 envelope 규칙을 따르는지 본다.
- PII·로그: 평문 PII 컬럼·Redis 키·로그 인자·예외 메시지를 찾는다. 새 민감 헤더·query가 `SensitiveLogMasker`, 프론트 `sentryUrl.ts`, 서버 Sentry 이벤트의 요청 헤더·query에서 모두 빠지는지 확인한다.
- 결제: prepare는 가격·최종 금액을 받지 않고 쿠폰 ID·적립금은 서버가 검증한다. confirm 금액·paymentKey는 `PaymentAttempt`가 비교하고, Toss 웹훅 payload는 PG 조회로만 상태를 확정한다.
- 외부 입력: OAuth redirect URI·state는 서버 고정, 로그인 뒤 이동은 `resolveSafeReturnTo`만 쓴다. 배송 웹훅은 서명 검증 후 파싱한다. 업로드·공개 이미지는 review 스킬과 대조하고 저장 파일명은 UUID 형식만 읽는지 본다.
- 프론트: CSP가 Report-Only라 `dangerouslySetInnerHTML`·`innerHTML`·외부 URL 이동을 막는 장치는 React escaping과 검증 helper뿐이다. 번들에 공개되는 `VITE_*`에 비밀값을 넣지 않는다.
- 비밀값·배포: 새 Secret key는 `create-secrets.sh` 허용 목록, 운영 불변값은 `ProductionRuntimeGuard`, 외부 노출은 `validate.sh`가 막는지 본다. Actuator는 Spring Security도 GET을 허용해 관리 포트·NetworkPolicy가 유일한 경계다.

## 위협 모델 요청 시

- 절차·공격자 모델·우선순위 기준은 [위협 모델 절차](references/threat-model.md)를 읽는다.
- 결과는 응답으로 제공한다. 보관을 원할 때만 저장소 문서 규칙을 따르고 임시 보고서 파일을 만들지 않는다.

## 보고와 검증

- 결과는 심각도 순으로 `파일:라인`, 악용 시나리오(전제→단계→영향), 기존 통제 근거, 권고를 나눠 쓴다. 확인하지 못한 전제는 확신도로 표시하고, diff 밖 기존 결함은 별도 절로 분리한다. 비밀값·token 원문은 위치만 적는다.
- 수정 검증: 필터·인가·CSRF `SecurityBoundaryUseCaseIT`, 처리율 `RateLimitFilterTest`·`SubjectRateLimitGuardTest`, 비회원 token `GuestTokenServiceTest`·`AccessTokenSignerTest`, 마스킹 `SensitiveLogMaskerTest`, 웹훅 `DeliveryApiWebhookVerifierTest`·`DefaultPaymentWebhookServiceTest`, 운영 불변값 `ProductionRuntimeGuardTest`, 프론트 Sentry는 `frontend`의 `npm run test:unit`을 선택한다. 업로드는 review 스킬의 사진 처리 검사를 쓴다.
- `adapter-in-web`의 `*UseCaseIT`는 `./gradlew --no-daemon :adapter-in-web:test --tests "*클래스명*"`으로 실행한다. 인증·결제 흐름은 원본 스킬의 검증 목록을 함께 선택한다.
