# Spring Security 전면 전환 검토

> 현재 커스텀 필터 체인(RequestIdFilter → RateLimitFilter → AdminAuthFilter → CustomerAuthFilter)을 Spring Security `SecurityFilterChain` 기반으로 전면 전환하는 시나리오를 검토한다.

---

## 배경

현재 인증·인가는 서블릿 필터 4개를 `@Order`로 직접 배치하는 방식이다. 관리자는 Redis 세션 토큰(Bearer), 회원은 Spring Session 쿠키(HG_SESSION)를 사용한다. 이 구조에서 Google 소셜 로그인을 추가하면서 Spring Security 도입 여부를 검토했고, 현 시점에서는 **커스텀 필터 유지 + RestClient 직접 OAuth2 구현(경로 B)** 을 선택했다.

이 문서는 향후 Spring Security 전면 전환이 필요해지는 시점과 전환 시나리오를 정리한다.

---

## 전환이 필요해지는 시점

| 트리거 | 이유 |
|--------|------|
| OAuth2 provider 3개 이상 | provider별 RestClient 코드가 반복되며, `spring-boot-starter-oauth2-client`의 자동 설정이 효율적 |
| 역할 기반 권한(RBAC) 복잡화 | 관리자 등급별 API 접근 제어가 필요하면 `@PreAuthorize`, `GrantedAuthority` 모델이 유리 |
| 멀티 서비스 인증 | JWT Resource Server가 필요하거나, 서비스 간 토큰 전파가 요구될 때 |
| 보안 헤더 일괄 적용 | CSP, X-Frame-Options 등을 Security 프레임워크로 통합 관리할 때 |

---

## 전환 범위

### 이전 대상 필터

| 현재 필터 | Security 대응 | 비고 |
|-----------|-------------|------|
| `RequestIdFilter` | `SecurityFilterChain` 앞단 커스텀 필터로 유지 | Security와 무관, 그대로 둘 수 있음 |
| `RateLimitFilter` | `SecurityFilterChain` 앞단 커스텀 필터로 유지 | 동일 |
| `AdminAuthFilter` | `AuthenticationProvider` + Bearer 토큰 처리 | `SecurityContext`에 관리자 인증 정보 저장 |
| `CustomerAuthFilter` | Spring Session + `SessionAuthenticationStrategy` | `SecurityContext`에 회원 인증 정보 저장 |

### 추가 작업

- `@EnableWebSecurity` + `SecurityFilterChain` 빈 구성
- `AuthenticationEntryPoint` 커스터마이즈 (현재 ErrorResponse JSON 형식 유지)
- `SecurityContextHolder`에서 인증 정보 조회하도록 컨트롤러 수정 (기존 `request.getAttribute()` → `@AuthenticationPrincipal`)
- MockMvc 테스트에 `@WithMockUser` 또는 `SecurityMockMvcRequestPostProcessors` 적용
- CORS 설정을 `SecurityFilterChain.cors()` 로 이동

---

## 예상 비용

- 필터 4개 재작성 + SecurityConfig 신규 작성
- 컨트롤러 인증 참조 방식 전면 변경 (`request.getAttribute` → `SecurityContext`)
- 기존 통합 테스트 전면 수정 (MockMvc 보안 설정)
- Spring Boot 4.0.2에서 `@AutoConfigureMockMvc` 미지원 → 테스트 설정 추가 복잡도

---

## 현재 보류 사유

1. 커스텀 필터 체인이 안정적으로 동작하고 있으며, 역할은 명확히 분리되어 있다.
2. 소셜 로그인은 Google 단일 provider로 RestClient 직접 구현이 더 간단하다.
3. RBAC 요구사항이 없다 (관리자 단일 등급).
4. 전환 시 기존 테스트 코드 전면 수정이 필요하여 리스크가 크다.
