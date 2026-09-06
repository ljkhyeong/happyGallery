---
name: happygallery-admin-flows
description: happyGallery의 관리자 인증·세션·필터·초기 설정·운영자 API를 변경할 때 사용한다. 화면만 바뀌면 frontend 스킬, 업무 정책이 주목적이면 해당 도메인 스킬을 사용한다.
---

# happyGallery 관리자 API·인증

## 규칙

- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/`와 `security/admin/`, ADR-0016·0017·0023을 확인한다.
- `RequestIdFilter → RateLimitFilter → 관리자 SecurityFilterChain` 순서를 유지한다. chain 안에서 `AdminAuthenticationFilter`는 `AnonymousAuthenticationFilter`보다 먼저 실행한다.
- 인증은 Redis Bearer 세션과 `SecurityContext`의 `AdminPrincipal`을 사용한다. `X-Admin-Key`는 명시적으로 켠 local/test fallback이며 관리자 신원 헤더로 쓰지 않는다.
- 계정명 기반 강제 잠금은 도입하지 않는다. 로그인 남용은 IP 제한·MFA·유효하지 않은 자격 증명의 일정한 BCrypt 작업·HMAC 감사 이력으로 제어한다. 과거 `LOGIN_BLOCKED` 이력은 계속 읽을 수 있어야 한다.
- 비밀번호 로그인 facade는 `Propagation.NEVER`를 유지한다. `AdminLoginSnapshot` 조회와 BCrypt 검사를 트랜잭션 밖에서 수행한 뒤 관리자 행을 잠가 변경된 자격 정보를 재확인하고 세션·MFA·감사 결과를 저장한다.
- 최초 설정은 설정값·빈 관리자 테이블·상수 시간 token 비교로 보호한다. 사용할 수 없는 설정 API는 404를 반환한다.
- 탈퇴 후 완료·취소 기록은 전용 history 조회로 읽는다. 익명화된 회원을 비회원으로 분류하거나 삭제된 개인정보를 복원하지 않는다.
- 브라우저 로그아웃은 로컬 token과 관리자 query cache를 먼저 지우고 서버 세션 삭제를 시도한다.

## 검증

- 인증·필터는 변경에 맞게 `SecurityBoundaryUseCaseIT`, `AdminLoginUseCaseIT`, `RateLimitFilterTest`를 선택한다. 초기 설정은 `AdminSetupControllerTest`·`DefaultAdminSetupServiceTest`를 선택한다.
- HTTP 계약은 `api-contract`를 적용하고 기존 `AdminAuthCatalogApiRestDocsTest`, `AdminBookingOrderApiRestDocsTest`, `AdminDashboardContentApiRestDocsTest`, `AdminOperationsApiRestDocsTest` 중 담당 클래스에 추가한다.
