# Hexagonal Architecture Transition Plan

현재 `happyGallery`는 `app / domain / infra / common` 모듈 분리는 이미 되어 있다.  
다만 애플리케이션 서비스가 Spring Data repository나 외부 연동 구현을 직접 알고 있는 구간이 많아서,
레이어드 아키텍처에서 헥사고날 아키텍처로 한 단계 더 명시적으로 전환하려면
`port / adapter` 경계를 점진적으로 세워야 한다.

이 문서는 현재 구조를 유지한 채, big-bang 재작성 없이
헥사고날 구조로 옮겨가기 위한 작업 단위와 규칙을 정의한다.

관련 ADR:
- `docs/ADR/0021_hexagonal-architecture-transition/adr.md`

현재 pilot 상태:
- `customer auth`, `guest claim`, `admin session`, `booking`, `order`, `payment`, `notification`, `product` 일부 경계에 `port/in`, `port/out`, `*PortAdapter` 도입 시작
- 다만 구현체 이름은 아직 기존 서비스명을 유지하는 구간이 있으며, 이번 전환은 의존 방향 정리를 우선하고 대규모 rename은 후순위로 둔다.

---

## 1. 목표

- `domain`은 순수 비즈니스 규칙만 가지게 한다.
- `app`은 유스케이스 orchestration과 `port` 정의를 담당한다.
- `infra`는 DB, 결제, 알림, 세션, 외부 시스템 구현체를 담당한다.
- 컨트롤러/배치/스케줄러는 inbound adapter로 분리해서 읽히게 만든다.
- “전부 인터페이스화” 대신, 경계가 있는 의존성만 port로 뽑는다.

---

## 2. 현재 구조 진단

현재 강점:
- `app / domain / infra / common` 모듈 분리가 이미 되어 있다.
- 도메인 정책과 외부 연동 구현이 완전히 한 곳에 섞여 있지는 않다.
- 기능 흐름별 통합 테스트가 꽤 잘 깔려 있다.

현재 한계:
- `app` 서비스가 `infra`의 Spring Data repository를 직접 의존하는 경우가 많다.
- 외부 연동 경계와 persistence 경계가 “기술 인터페이스” 수준이지, “애플리케이션 포트” 수준으로는 명확하지 않다.
- use case 경계가 컨트롤러/배치 진입점 기준으로 명시되지 않은 서비스가 많다.
- 일부 서비스 명이 orchestration/use case인지 내부 helper인지 이름만 보고 구분되지 않는다.

---

## 3. 핵심 원칙

- 헥사고날 전환은 점진적으로 한다. 한 번에 전체 패키지를 갈아엎지 않는다.
- `서비스 = 무조건 인터페이스` 규칙은 금지한다.
- 인터페이스는 adapter가 의존해야 하는 경계에만 만든다.
- 외부 시스템, persistence, 세션 저장소, 메시징, 알림, 결제 같은 것은 `out port` 후보다.
- controller, batch, scheduler, admin/manual trigger는 `inbound adapter`다.
- pure internal helper/service는 concrete class로 둬도 된다.

---

## 4. 네이밍 규칙

### 4.1 Port

- inbound port: `...UseCase`
  - 예: `CreateGuestBookingUseCase`
  - 예: `LoginCustomerUseCase`
- outbound port: `...Port`
  - 예: `OrderReaderPort`
  - 예: `OrderStorePort`
  - 예: `PaymentPort`
  - 예: `NotificationPort`
  - 예: `CustomerSessionPort`

### 4.2 Implementation

- 구현체 접미사 `Impl`은 사용하지 않는다.
- 새로 분리하거나 rename하는 인터페이스 구현체는 `Default` 접두사를 사용한다.
  - 예: `DefaultCreateGuestBookingUseCase`
  - 예: `DefaultLoginCustomerUseCase`
  - 예: `DefaultGuestClaimUseCase`
- 기존 운영 중 서비스는 pilot 단계에서 이름을 일괄 변경하지 않아도 된다.
  - 예: `CustomerAuthService implements CustomerAuthUseCase`
  - 예: `GuestClaimService implements GuestClaimUseCase`
  - 이후 해당 클래스를 별도 use case 구현체로 rename할 때 `Default*` 규칙을 적용한다.
- adapter 구현체는 역할이 드러나게 기술 접두어/접미어를 사용한다.
  - 예: `JpaOrderPersistenceAdapter`
  - 예: `JpaCustomerSessionAdapter`
  - 예: `FakeNotificationAdapter`
  - 예: `PgPaymentAdapter`

### 4.3 금지

- `FooServiceImpl`
- `BarRepositoryImpl`
- 의미 없는 `CommonService`, `UtilService`

---

## 5. 권장 패키지 구조

현재 모듈은 유지하고, 내부 패키지만 점진적으로 정리한다.

권장 방향:

- `app/.../port/in/**`
- `app/.../port/out/**`
- `app/.../usecase/**` 또는 기존 feature 패키지 안의 `Default*`
- `app/.../web/**`
- `app/.../batch/**`
- `infra/.../persistence/**`
- `infra/.../external/**`
- `infra/.../adapter/**`

예시:

```text
app
  customer
    port
      in
        LoginCustomerUseCase.java
      out
        CustomerSessionPort.java
        UserReaderPort.java
        UserStorePort.java
    usecase
      DefaultLoginCustomerUseCase.java
    web
      CustomerAuthController.java

infra
  customer
    persistence
      JpaUserRepository.java
      JpaCustomerSessionRepository.java
      JpaCustomerAuthAdapter.java
```

---

## 6. 무엇을 인터페이스화할지

### 6.1 반드시 port 후보인 것

- 결제 연동
- 알림 발송
- 고객/관리자 세션 저장소
- repository/persistence 접근
- 파일 저장/외부 API/메시징
- 모니터링/감사 로그처럼 향후 구현 교체 가능성이 있는 외부 경계

### 6.2 필요할 때만 interface로 두는 것

- controller가 직접 호출하는 use case
- batch/scheduler/manual trigger가 직접 호출하는 use case
- 다른 bounded context가 의존해야 하는 application service

### 6.3 굳이 interface로 만들지 않아도 되는 것

- 동일 feature 내부 private orchestration helper
- 하나의 use case 안에서만 쓰는 계산/조립 서비스
- domain policy를 보조하는 내부 validator/helper

---

## 7. 작업 단위

| 단위 | 성격 | 선행 | 핵심 산출물 |
|------|------|------|-------------|
| `H1` | 설계/문서 | 없음 | port/adaptor 규칙, 패키지 규칙, 네이밍 규칙 확정 |
| `H2` | 외부 연동 경계 | `H1` | 결제/알림/세션의 out port 추출 |
| `H3` | persistence pilot | `H1` | 하나의 도메인에서 repository 직접 의존 제거 |
| `H4` | inbound use case 명시화 | `H2`, `H3` | controller/batch가 `UseCase` 인터페이스를 호출하도록 정리 |
| `H5` | 도메인 확장 적용 | `H3` | order/booking/pass/product/customer 전반으로 port 확장 |
| `H6` | 검증/문서/정리 | `H1`~`H5` | 테스트 전략, ADR/README/HANDOFF, 남은 기술부채 정리 |

---

## 8. Task 운영 규칙

- 한 에이전트는 기본적으로 task 1개만 맡는다.
- persistence task와 외부 연동 task는 write scope가 겹치면 병렬 배정하지 않는다.
- `H3`, `H5`처럼 구조를 건드리는 task는 기능 변경을 섞지 않는다.
- task 완료 보고는 `변경 파일 / 경계 결정 / 검증 / 남은 직접 의존` 형식으로 남긴다.
- 새 구현체나 rename에는 `Default*` 규칙을 지키되, pilot 단계에서 기존 서비스명 유지 자체를 blocker로 삼지는 않는다.
- 실익 없는 interface 도입은 금지한다.

---

## 9. 단위별 상세

### H1. Architecture Baseline

목표:
- 현재 구조에서 헥사고날 전환 기준선을 문서로 고정한다.
- `in port / out port / adapter / Default*` 규칙을 팀 규칙으로 확정한다.

포함:
- 신규 1Pager
- 필요 시 ADR 초안
- 패키지/네이밍 예시

완료 기준:
- 다른 에이전트가 “무엇을 인터페이스화해야 하는지”를 문서만 보고 판단 가능하다.

#### H1 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `H1-T1` | 용어/경계 기준 정리 | 1Pager/ADR 문서 | 없음 | 가능 |
| `H1-T2` | 네이밍 규칙 확정 (`UseCase`, `Port`, `Default*`) | 1Pager/ADR/HANDOFF | `H1-T1` | 가능 |
| `H1-T3` | 권장 패키지 구조 예시 정리 | 1Pager 문서 | `H1-T1` | 가능 |
| `H1-T4` | pilot 범위와 금지사항 고정 | 1Pager/HANDOFF | `H1-T1` | 가능 |

task 완료 기준:
- `H1-T1`: port/adaptor 전환 기준선이 문서로 고정됐다.
- `H1-T2`: `Impl` 금지, `Default*` 규칙이 명확해졌다.
- `H1-T3`: 다른 에이전트가 새 패키지를 어디에 만들지 안다.
- `H1-T4`: big-bang 전환 금지와 pilot 범위가 명시됐다.

### H2. External Boundary First

목표:
- persistence보다 먼저 외부 경계를 port로 분리한다.

우선 대상:
- `payment`
- `notification`
- `customer/admin session`
- 필요 시 `client monitoring`

이유:
- 교체 가능성이 높고 경계가 명확하다.
- DB entity/JPA와 엮인 persistence보다 먼저 분리해도 위험이 낮다.

완료 기준:
- application service가 구체 구현이 아니라 `PaymentPort`, `NotificationPort`, `CustomerSessionPort` 같은 port를 본다.

#### H2 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `H2-T1` | 결제 port 추출 | `app/payment`, `infra/payment` | `H1` | 다른 H2 task와 병렬 가능 |
| `H2-T2` | 알림 port 추출 | `app/notification`, `infra/notification` | `H1` | 다른 H2 task와 병렬 가능 |
| `H2-T3` | 고객 세션 port 추출 | `app/customer`, `infra/user` 또는 session 관련 패키지 | `H1` | 다른 H2 task와 병렬 가능 |
| `H2-T4` | 관리자 세션 port 추출 | admin auth/session 관련 패키지 | `H1` | 다른 H2 task와 병렬 가능 |
| `H2-T5` | 필요 시 monitoring/audit port 추출 | monitoring/logging 관련 경계 | `H1` | 제한적 |

task 완료 기준:
- `H2-T1`: payment 유스케이스가 기술 구현 대신 `PaymentPort`를 본다.
- `H2-T2`: notification 유스케이스가 구현체 대신 `NotificationPort`를 본다.
- `H2-T3`: customer auth가 세션 저장 구현을 직접 모른다.
- `H2-T4`: admin auth도 같은 패턴을 따른다.
- `H2-T5`: 교체 가능성이 큰 부가 경계가 port로 빠졌다.

### H3. Persistence Pilot

목표:
- 한 도메인을 골라 repository 직접 의존을 port로 바꾼다.

권장 pilot:
- `customer auth + guest claim`
  - 경계가 비교적 작고
  - session/user/claim 흐름이 뚜렷하며
  - member store 전환과도 연결된다.

대안 pilot:
- `order`
- `booking`

권장 방식:
- `JpaRepository` 자체를 port로 노출하지 않는다.
- `UserReaderPort`, `UserStorePort`, `GuestClaimQueryPort`처럼 use case 관점 포트로 정의한다.

완료 기준:
- pilot 도메인에서 `app`이 `infra.*Repository`를 직접 import하지 않는다.

#### H3 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `H3-T1` | customer auth reader/store port 정의 | `app/customer/port/out` | `H1` | 불가 |
| `H3-T2` | guest claim query port 정의 | `app/customer/port/out` | `H1` | `H3-T1`과 병행 가능 |
| `H3-T3` | JPA adapter 구현 | `infra/.../adapter`, `infra/.../persistence` | `H3-T1`, `H3-T2` | 불가 |
| `H3-T4` | app 서비스에서 infra repository 직접 의존 제거 | `app/customer/**` | `H3-T3` | 불가 |
| `H3-T5` | customer auth + guest claim 회귀 테스트 정리 | 관련 test 패키지, 문서 메모 | `H3-T4` | 불가 |

task 완료 기준:
- `H3-T1`: user/session 관련 port가 생겼다.
- `H3-T2`: guest claim 조회 경계가 use case 관점으로 정의됐다.
- `H3-T3`: JPA 구현은 adapter 뒤로 숨겨졌다.
- `H3-T4`: `app`이 `infra.*Repository`를 직접 import하지 않는다.
- `H3-T5`: pilot 도메인 회귀를 테스트로 확인했다.

### H4. Inbound UseCase 명시화

목표:
- controller/batch/scheduler가 concrete service가 아니라 명시적 use case를 호출하게 만든다.

예시:
- `CustomerAuthController -> LoginCustomerUseCase`
- `AdminOrderController -> ApproveOrderUseCase`
- `PickupExpireBatchService -> ExpirePickupOrdersUseCase`

규칙:
- 구현체가 필요하면 `Default*` 이름을 쓴다.
- `Impl`은 쓰지 않는다.

완료 기준:
- 진입점 코드만 읽어도 “어떤 유스케이스를 호출하는지”가 바로 드러난다.

#### H4 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `H4-T1` | customer controller -> `UseCase` 정리 | `app/web/customer`, `app/customer/port/in` | `H2` or `H3` | 가능 |
| `H4-T2` | order admin controller -> `UseCase` 정리 | `app/web/admin`, `app/order/port/in` | `H2` | 가능 |
| `H4-T3` | booking/pass public controller -> `UseCase` 정리 | `app/web/booking`, `app/web/pass` | `H2` | 가능 |
| `H4-T4` | batch/scheduler -> `UseCase` 정리 | `app/batch`, `app/order`, `app/pass`, `app/booking` | `H2` | 가능 |

task 완료 기준:
- 각 진입점이 concrete service보다 `UseCase`를 본다.
- 구현체는 `Default*` 규칙을 따른다.

### H5. Domain-by-Domain Expansion

목표:
- pilot에서 확인된 패턴을 다른 도메인으로 확장한다.

권장 순서:
1. `customer`
2. `order`
3. `booking`
4. `pass`
5. `product`
6. `admin/batch`

주의:
- 한 번에 여러 도메인을 건드리지 않는다.
- 도메인별로 PR/커밋 단위를 끊는다.

완료 기준:
- 주요 도메인에서 adapter와 use case 경계가 일관된 패턴을 가진다.

#### H5 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `H5-T1` | order persistence/out port 확장 | `app/order`, `infra/order` | `H3` | 도메인별 병렬 가능 |
| `H5-T2` | booking persistence/out port 확장 | `app/booking`, `infra/booking` | `H3` | 도메인별 병렬 가능 |
| `H5-T3` | pass persistence/out port 확장 | `app/pass`, `infra/pass` | `H3` | 도메인별 병렬 가능 |
| `H5-T4` | product persistence/out port 확장 | `app/product`, `infra/product` | `H3` | 도메인별 병렬 가능 |
| `H5-T5` | admin/batch 경계 정리 | `app/web/admin`, `app/batch`, 관련 도메인 | `H4`, 관련 H5 task | 제한적 |

task 완료 기준:
- 각 도메인이 같은 port/adaptor 패턴을 따른다.
- 한 도메인 task가 다른 도메인의 write scope를 침범하지 않는다.

### H6. Verification And Cleanup

목표:
- 구조만 바뀌고 동작은 안 깨졌는지 확인한다.
- 남은 직접 의존 지점을 backlog로 남긴다.

포함:
- feature별 최소 테스트
- package dependency 재점검
- 문서/HANDOFF 정리

완료 기준:
- port/adaptor 전환된 도메인은 회귀 없이 통과한다.
- 남은 direct dependency 목록이 문서화된다.

#### H6 Task 분해

| Task | 목적 | 주 소유 범위 | 선행 | 병렬 |
|------|------|--------------|------|------|
| `H6-T1` | 변경 도메인별 최소 테스트 실행 | 관련 test 패키지 | 각 도메인 task 완료 후 | 가능 |
| `H6-T2` | 남은 direct dependency inventory 작성 | 1Pager/HANDOFF/추가 메모 | `H3` 이상 | 가능 |
| `H6-T3` | README/HANDOFF/ADR 후속 반영 | 문서 전반 | `H1`~`H5` | 가능 |
| `H6-T4` | package dependency / import 규칙 재점검 | 코드베이스 전반의 점검 메모 | `H3` 이상 | 가능 |

task 완료 기준:
- `H6-T1`: 구조 변경이 기능 회귀 없이 통과했다.
- `H6-T2`: 남은 직접 의존 구간이 backlog로 남았다.
- `H6-T3`: 다음 세션 문서가 최신이다.
- `H6-T4`: 재발 방지용 규칙이 정리됐다.

---

## 10. 추천 시작점

가장 추천하는 첫 실행 순서:

1. `H1` 문서/규칙 확정
2. `H2` 세션/알림/결제 port 추출
3. `H3` customer auth + guest claim persistence pilot

이 순서를 추천하는 이유:
- 전부 repository부터 바꾸면 JPA와 테스트 영향 범위가 너무 넓다.
- 외부 경계와 세션부터 분리하면 port 패턴을 안전하게 익힐 수 있다.
- customer auth/guest claim은 최근 member store 전환과 맞물려 있어 구조 개선 효과가 바로 보인다.

---

## 11. 병렬 배정 예시

- 에이전트 A: `H2-T1` 결제 port
- 에이전트 B: `H2-T2` 알림 port
- 에이전트 C: `H2-T3` 고객 세션 port
- 에이전트 D: `H3-T1` ~ `H3-T5` customer auth + guest claim pilot
- 에이전트 E: `H4-T2` admin order use case 경계

주의:
- `H3-T3`와 `H3-T4`는 같은 파일군을 만지므로 병렬 배정하지 않는다.
- `H5`는 도메인별 write scope가 겹치지 않을 때만 병렬 배정한다.

---

## 12. 하지 말아야 할 것

- 저장소 전체의 service를 일괄적으로 interface + 구현체로 쪼개기
- `*Impl` 이름을 기계적으로 도입하기
- `infra`의 Spring Data repository를 그대로 `domain`이나 `web`에 노출하기
- entity/JPA 세부사항을 port 계약에 그대로 새기기
- 구조 리팩토링과 기능 변경을 한 PR에서 같이 밀어넣기

---

## 13. 완료 기준

- 적어도 한 개 도메인에서 `port in / port out / adapter / Default*` 패턴이 안정적으로 정착한다.
- 외부 연동과 persistence 경계가 기술 구현이 아니라 application port 중심으로 읽힌다.
- controller/batch는 use case를 호출하고, use case는 port를 호출하며, infra는 port를 구현한다.
- `Impl` 없이도 구현체 네이밍이 일관되다.

---

## 14. 최소 검증

- `./gradlew --no-daemon :app:test --tests ...`
- 필요 시 `./gradlew --no-daemon :app:useCaseTest`
- 변경된 도메인에 맞는 frontend/build 또는 API smoke

---

## 15. 다른 에이전트에게 넘길 때 한 줄 지시

- `hexagonal plan의 H2만 진행해줘. payment/notification/session out port만 추출하고 Impl 대신 Default 접두사 규칙을 지켜줘.`
- `hexagonal plan의 H3만 진행해줘. customer auth + guest claim을 persistence pilot으로 정리하고, app이 infra repository를 직접 import하지 않게 해줘.`
- `hexagonal plan의 H4만 진행해줘. controller와 batch 진입점이 UseCase 인터페이스를 호출하도록만 정리해줘.`
