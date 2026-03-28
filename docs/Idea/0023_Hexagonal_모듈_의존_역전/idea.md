# 0023 — 헥사고날 모듈 의존 방향 역전

> **구현 완료** — `app → infra` 의존 제거, `infra → app` 구현 완료. 아키텍처 결정은 [ADR-0021](../../ADR/0021_헥사고날_아키텍처_채택/adr.md)에서 관리한다. 이 문서는 배경 기록으로만 유지한다.

## 현재 상태

```
common  ← (순수 유틸)
domain  → common
infra   → common, domain
app     → common, domain, infra   ← 역방향 의존
```

`app` 모듈이 `infra`를 직접 의존한다.
PortAdapter 클래스 20+개가 `app`에 위치하며 `infra`의 Repository를 import하여
`app → infra` 역방향 의존이 발생한다.

## 목표 상태

```
common  ← (순수 유틸)
domain  → common
infra   → common, domain, app     (어댑터가 포트를 구현)
app     → common, domain          (포트 인터페이스만 정의)
```

`app`은 포트 인터페이스를 정의하고, `infra`가 이를 구현한다.
`app → infra` 의존을 완전히 제거한다.

## 전환 단계

### 1단계: infra → app 의존 추가
- `infra/build.gradle`에 `implementation project(":app")` 추가
- 이 시점에서 양방향 의존이 일시적으로 존재함 (Gradle은 허용, 순환만 없으면 됨)

### 2단계: 단순 위임 어댑터 제거
- Repository가 Port를 직접 extends하는 방식으로 전환
- 예: `FulfillmentRepository extends JpaRepository<...>, FulfillmentPort`
- 대상: 1:1 단순 위임만 하는 PortAdapter (약 15~18개)

### 3단계: 복합/변환 어댑터를 infra로 이동
- 여러 Repository를 조합하는 어댑터 (`GuestClaimQueryPortAdapter` 등)
- 변환 로직이 있는 어댑터 (`NotificationSenderPortAdapter` 등)
- `app` → `infra` 패키지로 이동, import 수정

### 4단계: app → infra 의존 제거
- `app`에서 `infra` import가 0개인지 확인
- `app/build.gradle`에서 `implementation project(":infra")` 제거
- 컴파일 검증

## 향후 검토: 도메인 객체 ↔ JPA 엔티티 분리

현재 `domain` 모듈의 클래스가 `@Entity`, `@Table` 등 JPA 어노테이션을 직접 가진다.
순수 헥사고날에서는 도메인이 인프라 기술을 모르는 것이 원칙이지만, 현 단계에서는 분리하지 않는다.

### 분리하지 않는 이유
- 엔티티 ~20개 × (도메인 클래스 + JPA 엔티티 + Mapper) = 파일 3배 증가
- 도메인 모델과 DB 스키마가 1:1로 대응되어 매핑 계층의 실질 이득이 없음
- JPA 어노테이션은 메타데이터일 뿐 비즈니스 로직 동작을 바꾸지 않음
- 상태 전이 가드(`requireCancellable()` 등)가 enum에 응집되어 있어 도메인 순수성은 실질적으로 유지됨

### 분리를 재검토할 시점
- CQRS 읽기 모델 분리 또는 이벤트 소싱 도입 시
- 하나의 도메인 객체가 여러 테이블에 걸치는 구조가 될 때
- domain 모듈을 JPA 없는 환경(예: 다른 프로젝트)에서 재사용해야 할 때

### 분리 시 접근법 (참고)
- MapStruct 또는 수동 매퍼로 `domain.Booking` ↔ `infra.BookingEntity` 변환
- Repository가 JPA 엔티티를 다루고, Port 구현체에서 도메인 객체로 변환하여 반환
- 점진적 전환: 변경이 잦은 도메인부터 하나씩 분리

## 주의사항

- Spring Data JPA의 `JpaRepository` 메서드 시그니처와 Port 메서드가 충돌할 수 있음
  - `save(T)` 반환 타입, `findById(ID)` 등은 대부분 호환
  - 커스텀 쿼리 메서드명이 Port와 다르면 `default` 메서드로 브릿지 가능
- `@SpringBootApplication`의 component scan 범위 확인 필요
  - `infra` 패키지가 scan 대상에 포함되어야 함
- 웹 컨트롤러는 현재 `app` 모듈에 있으므로 별도 `web` 모듈 분리는 이번 범위 밖
