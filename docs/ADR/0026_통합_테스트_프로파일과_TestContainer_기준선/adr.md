# ADR-0026: 통합 테스트 프로필과 Testcontainers 사용 기준

**날짜**: 2026-03-20  
**상태**: Accepted

---

## 왜 이 문서가 필요한가

통합 테스트는 컨트롤러만 호출해 보는 수준이 아니라, Flyway, JPA, Redis 세션, 필터 체인까지 포함한 실제 동작을 검증해야 한다.  
테스트마다 프로필과 설정이 제각각이면 실행 비용이 커지고 결과 해석도 어려워진다.

---

## 결정

### 1. 유스케이스 통합 테스트의 기본 진입점은 `@UseCaseIT`다

- `@UseCaseIT`는 `@SpringBootTest` 기반 전체 컨텍스트를 로드한다.
- 기본 프로필은 `test`
- 기본 `MockMvc` 설정은 `addFilters = false`

### 2. 통합 테스트 인프라는 Testcontainers를 사용한다

- MySQL: `MySQLContainer("mysql:8.0")`
- Redis: `GenericContainer("redis:7-alpine")`
- 두 컨테이너 모두 `@ServiceConnection`으로 연결한다.

### 3. 공통 테스트 설정은 `application-test.yml`에 모은다

- 테스트 전용 관리자 API key
- rate limit 비활성화
- 로그 레벨 조정

이런 공통 설정은 테스트 클래스마다 `@TestPropertySource`로 흩뿌리지 않는다.

### 4. 필터 검증은 필요할 때만 수동으로 `MockMvc`를 조립한다

- `@AutoConfigureMockMvc(addFilters = true)`처럼 컨텍스트 캐시에 영향을 주는 방식은 기본값으로 두지 않는다.
- 필요한 필터만 `MockMvcBuilders.webAppContextSetup(...).addFilters(...)`로 붙인다.

### 5. `adapter-in-web` 테스트는 `application`의 test fixtures를 재사용한다

- 공통 테스트 인프라는 `application/src/testFixtures/**`에 둔다.
- `adapter-in-web`는 `testFixtures(project(":application"))`를 사용해 같은 설정을 재사용한다.

---

## 결과

### 장점

- MySQL과 Redis를 운영과 비슷한 방식으로 검증할 수 있다.
- 테스트 설정이 한 곳으로 모여 관리가 쉬워진다.
- 불필요한 컨텍스트 분리를 줄일 수 있다.

### 단점

- 컨테이너 기동 비용 때문에 단위 테스트보다 느리다.

### 대응

- 핵심 흐름만 `@UseCaseIT`로 검증하고, 빠른 정책 테스트는 별도로 둔다.

---

## 구현 반영

- `application/src/testFixtures/java/com/personal/happygallery/support/UseCaseIT.java`
- `application/src/testFixtures/java/com/personal/happygallery/support/TestcontainersConfig.java`
- `application/src/testFixtures/resources/application-test.yml`
- `adapter-in-web/src/test/java/**`

---

## 참고 문서

- `docs/Idea/0014_테스트_Context_공유와_Profile_분리/idea.md`
- `docs/Idea/0013_회원_세션_Spring_Session_전환_검토/idea.md`
- `docs/Idea/0015_다중_인스턴스용_Redis_도입/idea.md`
