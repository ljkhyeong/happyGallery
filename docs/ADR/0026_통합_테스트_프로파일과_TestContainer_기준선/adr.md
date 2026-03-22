# ADR-0026: 통합 테스트 프로파일 및 Testcontainers 기준선

**날짜**: 2026-03-20  
**상태**: Accepted

---

## 컨텍스트

통합 테스트는 단순히 컨트롤러만 호출해 보는 용도가 아니다.
Flyway 마이그레이션, JPA 매핑, Redis 세션/레이트리밋, 필터 체인까지 포함한 실제 애플리케이션 동작을 검증해야 한다.

기존에는 `local` 프로필 혼용, 테스트 클래스별 프로퍼티 override, `MockMvc` 필터 설정 차이 때문에
테스트 컨텍스트가 불필요하게 나뉘고 실행 비용도 커졌다.

따라서 통합 테스트는 운영과 비슷한 인프라를 쓰되, 공통 프로파일과 컨텍스트 기준을 문서로 고정한다.

---

## 결정 사항

### 1. 유스케이스 통합 테스트의 표준 엔트리포인트는 `@UseCaseIT`이다

- `@UseCaseIT`는 `@SpringBootTest` 기반 전체 컨텍스트를 로드한다.
- 기본 프로파일은 `test`를 사용한다.
- 기본 `MockMvc` 설정은 `addFilters = false`로 두고, 필요한 필터는 테스트 코드에서 명시적으로 조립한다.

### 2. 통합 테스트 인프라는 Testcontainers를 기준으로 한다

- MySQL은 `MySQLContainer("mysql:8.0")`로 기동한다.
- Redis는 `GenericContainer("redis:7-alpine")`로 기동한다.
- 두 컨테이너 모두 Spring Boot `@ServiceConnection`으로 연결한다.
- 이렇게 해서 Flyway, JPA, Spring Session, Redis 기반 rate limit 경로를 실제와 가깝게 검증한다.

### 3. 공통 테스트 설정은 `application-test.yml`에 둔다

- 테스트 전용 관리자 API key, rate limit 비활성화, 로그 레벨 조정 등 공통 설정은 `application-test.yml`에 둔다.
- 공통 설정을 위해 `@TestPropertySource`를 각 테스트 클래스에 반복해서 사용하지 않는다.

### 4. 필터 검증이 필요한 테스트는 수동 `MockMvc` 조립 패턴을 사용한다

- `@AutoConfigureMockMvc(addFilters = true)`처럼 컨텍스트 캐시에 영향을 주는 클래스별 차등 설정은 피한다.
- 필요한 필터만 `MockMvcBuilders.webAppContextSetup(...).addFilters(...)` 패턴으로 붙인다.
- 목적은 필터 동작 검증과 컨텍스트 공유를 동시에 만족하는 것이다.

---

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | MySQL/Redis 의존 흐름을 운영과 유사한 환경에서 검증할 수 있다 |
| 장점 | `test` 프로필과 공통 설정 파일을 기준으로 테스트 구성이 단순해진다 |
| 장점 | 불필요한 컨텍스트 분리가 줄어 전체 테스트 비용을 낮출 수 있다 |
| 단점 | 컨테이너 기동 비용이 있어 단위 테스트보다 느리다 |
| 대응 | `@UseCaseIT` 범위를 통합 흐름 검증으로 제한하고, 정책 테스트는 별도 `policyTest`로 유지한다 |

---

## 구현 반영

- `app/src/test/java/com/personal/happygallery/support/UseCaseIT.java`
- `app/src/test/java/com/personal/happygallery/support/TestcontainersConfig.java`
- `app/src/test/resources/application-test.yml`
- 필터 검증 테스트의 수동 `MockMvc` 조립 패턴

---

## 참고 문서

- `docs/Idea/0014_테스트_Context_공유와_Profile_분리/idea.md`
- `docs/Idea/0013_회원_세션_Spring_Session_전환_검토/idea.md`
- `docs/Idea/0015_다중_인스턴스용_Redis_도입/idea.md`
