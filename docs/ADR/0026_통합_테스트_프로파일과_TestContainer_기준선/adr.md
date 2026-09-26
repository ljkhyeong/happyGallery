# ADR-0026: 통합 테스트 프로필과 Testcontainers 사용 기준

**날짜**: 2026-03-20  
**상태**: Accepted
**갱신**: 2026-09-27

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
- `SharedTestContainers`가 테스트 JVM마다 서버를 한 번 기동한다. `TestcontainersConfig`의 `DynamicPropertyRegistrar`가 할당된 DB의 연결 정보를 등록한다.
- context마다 별도 MySQL DB와 Redis DB 번호를 배정한다. migration 검사는 클래스마다 별도 MySQL DB를 받아 `clean()`과 이전 버전 migration이 다른 검사에 영향을 주지 않게 한다.
- 컨테이너를 Spring Bean이나 JUnit `@Container`로 등록하지 않는다. 개별 context·클래스 종료 시 서버를 닫지 않고, JVM 종료 후 Testcontainers의 Ryuk이 정리한다. 실행 간 컨테이너 재사용 옵션은 켜지 않는다.
- CI의 별도 실행기·테스트 JVM 사이에서는 서버를 공유하지 않는다.

### 3. 공통 테스트 설정은 `application-test.yml`에 모은다

- 테스트 전용 관리자 API key
- rate limit 비활성화
- 로그 레벨 조정
- Spring Session Redis cleanup cron 비활성화(`spring.session.data.redis.cleanup-cron: "-"`)

이런 공통 설정은 테스트 클래스마다 `@TestPropertySource`로 흩뿌리지 않는다.

Redis cleanup cron은 인덱스 보정용 백그라운드 작업이며, 세션 TTL과 키스페이스 만료 이벤트는 이 설정과 무관하게 유지된다.
짧게 살았다 종료되는 Testcontainers 컨텍스트에서는 cleanup scheduler가 Redis 연결 팩토리 종료와 경합해 오류 로그를 남길 수 있으므로, `test` 프로필에서만 Spring Session이 지원하는 비활성 값 `-`를 사용한다.

### 4. 필터 검증은 필요할 때만 수동으로 `MockMvc`를 조립한다

- `@AutoConfigureMockMvc(addFilters = true)`처럼 컨텍스트 캐시에 영향을 주는 방식은 기본값으로 두지 않는다.
- 필요한 필터만 `MockMvcBuilders.webAppContextSetup(...).addFilters(...)`로 붙인다.

### 5. 공통 fixture는 의존 대상에 따라 소유 모듈을 나눈다

- 애플리케이션 포트와 도메인 타입만 사용하는 fixture, `@UseCaseIT`, Testcontainers 설정은
  `application/src/testFixtures/**`에 둔다.
- 웹 DTO, `MockMvc`, 영속성 repository를 사용하는 요청 helper, 상태 probe, DB 정리 지원은
  `test-support/src/testFixtures/**`에 둔다.
- `application`과 `adapter-in-web` 테스트는 필요한 두 test fixtures variant를 함께 사용한다.
- `test-support`는 테스트 classpath 전용 보조 모듈이며 운영 산출물에는 포함하지 않는다.

### 6. DB 정리는 테스트가 사용하는 도메인 범위로 제한한다

- 테스트 클래스는 Repository를 나열해 직접 삭제하지 않고 `TestCleanupSupport`의 도메인별 정리 메서드를 호출한다.
- DB 정리는 `@AfterEach`에서 테스트가 사용한 범위만 한 번 실행한다. `@BeforeEach`는 fixture, mock, `MockMvc` 설정에만 사용한다.
- 테스트 중간의 데이터 삭제는 알림 건수 기준점처럼 시나리오 의미가 있는 경우에만 명시적으로 남긴다.
- 모든 `@UseCaseIT` 실행 후 전체 테이블을 일괄 삭제하는 전역 정리는 사용하지 않는다.
- 비동기 작업을 발생시킨 테스트는 작업 완료를 기다린 뒤 관련 데이터를 정리한다. 주문 클레임 테스트는 알림 executor의 실행·대기 작업이 끝난 후 주문과 알림 이력을 정리한다. context 재사용을 늘릴 때 기존 정리 범위가 충분한지도 확인한다.

### 7. mock·spy 구성을 통일하되 업무 조건이 다른 context는 유지한다

- 같은 `@UseCaseIT`를 붙여도 mock·spy 구성이나 프로퍼티가 다르면 별도 context가 만들어진다.
- 외부 연동 응답을 제어하는 테스트는 `@ExternalIntegrationUseCaseIT`의 공통 mock 구성을 사용한다. 이 어노테이션이 대체하는 연동 전체가 해당 테스트의 검증 범위에 적합한지 먼저 확인한다.
- 실제 Bean의 호출·실패를 관찰하는 테스트는 `@SpiedUseCaseIT`의 공통 spy 구성을 사용한다. JPA repository는 일부 포트 인터페이스로 좁히면 다른 인터페이스를 요구하는 주입이 실패할 수 있으므로 실제 repository 타입을 지정한다. 이 설정은 영속성·외부 adapter 타입을 참조하므로 `test-support` fixture가 소유한다.
- 공통 mock·spy는 Spring의 테스트 종료 후 reset을 유지한다. 객체 내부 상태나 DB 데이터까지 reset되는 것으로 간주하지 않는다.
- 배송비·암호화 키·알림 실행 방식처럼 검증하려는 업무 조건이 다른 context는 합치지 않는다. 기동 횟수를 줄이기 위해 모든 테스트에 같은 mock을 강제하지 않는다.

### 8. 실행 시간은 같은 범위에서 측정한다

2026-09-27 로컬 Java 25·Docker 환경에서 아래 명령을 변경 전후 각각 실행했다. 비교 대상은 `ccce06dc`와 구현 커밋 `95526028`이다.

```bash
./gradlew --no-daemon :application:test --rerun-tasks --profile
```

| 항목 | 변경 전 | 변경 후 |
|---|---:|---:|
| 명령 전체 시간 | 5분 34초 | 3분 36초 |
| Spring context 기동 횟수 | 14회 | 10회 |
| context 기동 시간 합계 | 191.22초 | 90.98초 |
| 통과한 테스트 사례 | 777개 | 779개 |

기존 777개 사례는 모두 유지했고 MySQL·Redis 격리 회귀 2개를 추가했다. 웹 통합 테스트 97개, OpenAPI 일치 검사와 구조 검사도 통과했다. 로컬 전체 시간은 약 35% 줄었지만 단일 환경의 측정값이며, GitHub CI 전체 시간은 아직 측정하지 않았다. 시간 도약 경고가 있었던 중간 실패 실행은 비교에서 제외했다.

재측정할 때는 같은 JDK·Docker·테스트 범위를 사용하고, `build/reports/profile/`의 태스크 시간과 `application/build/test-results/test/`의 테스트 결과·Spring 기동 로그를 함께 확인한다. 테스트 누락이나 실패를 속도 개선으로 계산하지 않는다.

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
- `application/src/testFixtures/java/com/personal/happygallery/support/SharedTestContainers.java`
- `application/src/testFixtures/java/com/personal/happygallery/support/ExternalIntegrationUseCaseIT.java`
- `test-support/src/testFixtures/java/com/personal/happygallery/support/SpiedUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/support/SharedTestContainersTest.java`
- `application/src/testFixtures/resources/application-test.yml`
- `test-support/src/testFixtures/java/com/personal/happygallery/support/TestCleanupSupport.java`
- `test-support/src/testFixtures/java/com/personal/happygallery/support/*TestHelper.java`
- `test-support/src/testFixtures/java/com/personal/happygallery/support/*StateProbe.java`
- `adapter-in-web/src/test/java/**`

---

## 참고 문서

- `docs/Idea/0014_테스트_Context_공유와_Profile_분리/idea.md`
- `docs/Idea/0013_회원_세션_Spring_Session_전환_검토/idea.md`
- `docs/Idea/0015_다중_인스턴스용_Redis_도입/idea.md`
