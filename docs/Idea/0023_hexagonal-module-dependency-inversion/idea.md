# 0023 — 헥사고날 모듈 의존 방향 역전

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

## 주의사항

- Spring Data JPA의 `JpaRepository` 메서드 시그니처와 Port 메서드가 충돌할 수 있음
  - `save(T)` 반환 타입, `findById(ID)` 등은 대부분 호환
  - 커스텀 쿼리 메서드명이 Port와 다르면 `default` 메서드로 브릿지 가능
- `@SpringBootApplication`의 component scan 범위 확인 필요
  - `infra` 패키지가 scan 대상에 포함되어야 함
- 웹 컨트롤러는 현재 `app` 모듈에 있으므로 별도 `web` 모듈 분리는 이번 범위 밖
