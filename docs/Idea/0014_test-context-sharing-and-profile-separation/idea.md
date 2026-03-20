# 통합 테스트 컨텍스트 공유 및 프로파일 분리

**날짜**: 2026-03-18
**상태**: ADR 반영 완료

> 최종 채택 기준은 [ADR-0026](../../ADR/0026_integration-test-profile-and-testcontainers-baseline/adr.md) 확인. 이 문서는 테스트 전환 배경과 검토 맥락 보존용이다.

---

## 배경

통합 테스트 실행 속도가 느린 원인을 점검하던 중, `AdminSlotUseCaseIT`가 다른 테스트와 Spring ApplicationContext를 공유하지 않고 별도 컨텍스트를 띄우고 있음을 발견했다.

---

## 문제 분석

Spring은 ApplicationContext를 캐싱할 때, 컨텍스트 구성에 영향을 주는 메타데이터를 캐시 키로 사용한다. `@TestPropertySource`의 프로퍼티 값, `@AutoConfigureMockMvc`의 속성, `@ActiveProfiles` 등이 모두 키에 포함된다. 하나라도 다르면 새로운 컨텍스트가 생성된다.

`AdminSlotUseCaseIT`는 `@UseCaseIT`를 사용하면서 두 가지를 추가로 덧붙이고 있었다.

```java
@UseCaseIT
@AutoConfigureMockMvc(addFilters = true)   // UseCaseIT의 addFilters = false 재정의
@TestPropertySource(properties = {         // 프로퍼티 오버라이드
    "app.admin.api-key=dev-admin-key",
    "app.admin.enable-api-key-auth=true"
})
class AdminSlotUseCaseIT { ... }
```

이 두 가지 차이로 인해 이 클래스만을 위한 컨텍스트가 별도로 생성되었고, 전체 통합 테스트 스위트에서 컨텍스트가 총 4개 기동되고 있었다.

| 그룹 | 조건 | 컨텍스트 |
|------|------|------|
| 순수 `@UseCaseIT` | mock 없음 | Context 1 |
| `@MockitoBean NotificationService` | 5개 클래스 공유 | Context 2 |
| `@MockitoBean PaymentProvider` | 2개 클래스 공유 | Context 3 |
| `AdminSlotUseCaseIT` | 단독 | Context 4 |

Context 2, 3은 Spring Framework 6.2+의 `@MockitoBean`이 bean override 조합을 캐시 키에 포함시켜 같은 조합끼리 공유하는 정상적인 동작이다. Context 4만 불필요한 분리였다.

---

## 해결 방향

### 1. `@TestPropertySource` 제거 → 공유 설정 파일로 이동

어드민 프로퍼티를 공유 설정에 넣으면 `@TestPropertySource`가 불필요해진다. 이 과정에서 기존에 `local` 프로파일을 테스트에도 혼용하던 구조를 정리하고 `test` 프로파일을 분리했다.

- `src/test/resources/application-test.yml` 신규 생성
- `@UseCaseIT`에 `@ActiveProfiles("test")` 추가
- 기존 `src/test/resources/application-local.yml` 제거 (test 프로파일 활성화 시 local 프로파일 비활성)

`application-test.yml`에는 어드민 프로퍼티 외에 rate limit 비활성화, 로그 레벨 조정 등 테스트 전용 설정을 관리한다.

```yaml
# application-test.yml
spring:
  config:
    activate:
      on-profile: test

app:
  admin:
    api-key: dev-admin-key
    enable-api-key-auth: true
  rate-limit:
    enabled: false

logging:
  level:
    root: WARN
    com.personal.happygallery: INFO
```

### 2. `@AutoConfigureMockMvc(addFilters = true)` 제거 → 수동 MockMvc 조립

`addFilters` 값을 클래스마다 달리 쓰면 컨텍스트 캐시가 깨진다. `MeBookingUseCaseIT` 등이 이미 사용하는 `MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build()` 패턴으로 통일했다. 이 패턴은 컨텍스트 구성에 영향을 주지 않으면서 테스트별로 필요한 필터만 선택적으로 적용할 수 있다.

---

## 결과

Context 4가 Context 1로 병합되어 컨텍스트가 4개에서 3개로 줄었다. 전체 통합 테스트 스위트 실행 시 Testcontainers MySQL 컨테이너를 한 번 덜 기동한다.

---

## 가이드

앞으로 통합 테스트를 추가할 때 컨텍스트 분리를 유발하지 않으려면 아래를 지킨다.

- `@TestPropertySource`를 사용하지 않는다. 테스트 전용 설정은 `application-test.yml`에 넣는다.
- 필터가 필요한 테스트는 `@AutoConfigureMockMvc(addFilters = true)`로 전환하지 않고 `MockMvcBuilders`로 직접 조립한다.
- `@MockitoBean`은 같은 타입을 mock하는 테스트끼리 컨텍스트를 공유하므로, mock 대상 타입을 늘리기보다 기존 그룹에 맞춰 작성한다.
