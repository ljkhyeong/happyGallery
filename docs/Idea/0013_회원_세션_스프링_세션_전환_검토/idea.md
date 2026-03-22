# 회원 세션의 Spring Session 전환 메모

**날짜**: 2026-03-17  
**상태**: ADR 반영 완료

> 최종 채택 기준은 [ADR-0023](../../ADR/0023_관리자_인증과_런타임_운영_기준선/adr.md) 확인. 이 문서는 전환 배경과 검토 맥락 보존용이다.

---

## 배경

기존 회원 인증은 `HG_SESSION` 쿠키 + `CustomerAuthFilter` + `UserSession`/`UserSessionRepository` 기반의 직접 구현 세션으로 동작했다.

이 구조는 프로젝트 초기에는 단순하고 통제하기 쉽지만,
세션 저장, 만료, 다중 인스턴스 확장, 표준 인증 체계와의 정렬까지 직접 책임져야 한다는 비용이 있다.

현재는 Spring Session + Redis로 전환해 세션 저장소와 만료 관리, 분산 환경 대응을 표준 방식으로 옮겼다.

---

## 적용 결과

- 회원 세션
  - 쿠키 이름: `HG_SESSION`
  - 진입점: `CustomerAuthFilter`
  - 저장소: Spring Session + Redis
  - 세션 attribute: `customerUserId`
  - 인증 서비스: `DefaultCustomerAuthService`
- 관리자 세션
  - Bearer 토큰 + Redis 기반 `AdminSessionStore`
  - 회원 세션과는 별도 모델 유지

회원 세션만 Spring Session으로 전환했고, 관리자 세션을 같은 프레임워크로 억지 통합하지는 않았다.

---

## 실제 효과

1. 세션 저장/조회/만료 처리의 표준화
2. Redis 기반 수평 확장 경로 확보
3. 세션 관련 커스텀 코드 축소
4. Spring Security/Spring MVC와의 궁합 개선

---

## 적용 시 고려한 점

### 1. 기존 계약 유지

- `HG_SESSION` 쿠키 이름을 그대로 유지했다.
- `/api/v1/me`와 `CustomerAuthFilter` 진입 계약은 유지하고, 세션 저장 구현만 바꿨다.

### 2. guest claim 흐름 회귀 방지

- 로그인 직후 `/my?claim=1`
- guest 성공 화면 → 회원가입/로그인 → claim 모달 자동 진입
- 전화번호 재인증 후 claim

이 흐름은 회원 세션 계약에 의존하므로 회귀 테스트에서 계속 확인해야 한다.

### 3. Redis 의존 추가

- Spring Session 저장소는 Redis를 사용한다.
- local은 `localhost:6379` 또는 docker compose `redis` 서비스를 사용한다.
- 테스트는 Testcontainers Redis를 함께 기동한다.

### 4. 관리자 세션과의 경계 유지

- 관리자 세션은 Bearer 토큰 기반을 유지한다.
- 이번 변경은 회원 세션 저장 표준화에 집중했다.

---

## 적용 방식

1. **회원 세션만** Spring Session 전환 대상으로 잡았다.
2. `HG_SESSION` 쿠키 계약은 유지했다.
3. 관리자 세션(`AdminSessionStore`)은 별도 유지했다.
4. 저장/만료 표준화에 집중하고, 인증 프레임워크 전면 교체는 범위 밖으로 뒀다.
5. `CustomerAuthFilter`는 세션 조회만 담당하도록 단순화했다.

---

## 구현 요약

### 세션 생성/종료

- `CustomerAuthController`가 로그인/회원가입 시 `HttpSession`에 `customerUserId`를 기록한다.
- 로그아웃은 Spring Session `HttpSession.invalidate()`로 처리한다.

### 세션 검증

- `CustomerAuthFilter`는 Spring Session filter 이후 실행되며, 세션의 `customerUserId`로 사용자를 다시 조회한다.
- 기존 `UserSession` 엔티티와 저장소 코드는 제거했다.

### 인프라/테스트

- `spring-session-data-redis`, `spring-boot-starter-data-redis`를 추가했다.
- `RedisConfig`에서 쿠키 이름을 `HG_SESSION`으로 고정했다.
- 통합 테스트는 Testcontainers Redis를 함께 사용한다.

---

## 현재 결론

회원 세션은 Spring Session + Redis로 전환되었고, `HG_SESSION` 계약은 유지했다.  
직접 구현 세션 삭제로 커스텀 코드가 줄었고, 다중 인스턴스 환경에서도 표준 세션 저장소를 사용할 수 있게 됐다.
