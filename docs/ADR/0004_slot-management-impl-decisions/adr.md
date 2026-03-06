# ADR-0004: 슬롯 관리 구현 수준 설계 선택

- **상태**: 확정
- **날짜**: 2026-02-22
- **관련 파일**:
  - `app/build.gradle`
  - `app/booking/SlotManagementService.java`
  - `app/web/admin/dto/SlotResponse.java`
  - `infra/booking/SlotRepository.java`

---

## Context

§5.1 슬롯 관리 구현 중 아키텍처 결정(ADR-0003) 외에도,
구현 레벨에서 여러 선택 지점이 있었다.
이 ADR은 각 선택의 이유와 트레이드오프를 기록한다.

---

## Decision 1: `app` 모듈에 `spring-boot-starter-data-jpa` 직접 선언

### 배경
`app` 모듈에서 `@Transactional`을 사용하려면 `spring-tx`가 필요하다.
`infra` 모듈이 `spring-boot-starter-data-jpa`를 `implementation` 스코프로 가지고 있지만,
Gradle `implementation`은 소비자(app)에게 전이되지 않는다.

### 결정
`app/build.gradle`에 `spring-boot-starter-data-jpa`를 직접 추가한다.

### 대안
- `infra/build.gradle`에서 `api` 스코프로 변경 → 전이 가능하나, JPA 구현 세부를 `app`에 노출하는 것이 명시적이지 않음
- `spring-tx`만 별도 선언 → 버전 관리가 분산됨. Spring Boot BOM 활용 위해 스타터 전체 사용이 편리

### 트레이드오프 / 위험
- `infra`와 `app` 모두 JPA 스타터 보유. Spring Boot는 단일 `EntityManagerFactory`로 통합하므로 런타임 충돌 없음.
- JPA 관련 설정(`spring.jpa.*`)은 `app` 모듈의 `application.yml`에서 관리. `infra`에서 설정하지 않는다.

---

## Decision 2: `SlotResponse.from(slot)` — OSIV 비활성화(`open-in-view=false`) 기준 유지

### 배경
현재 애플리케이션은 `spring.jpa.open-in-view=false`를 기본으로 사용한다.
`BookingClass`는 `Slot`에 `FetchType.LAZY`로 연관되어 있고,
`SlotResponse`에는 `classId` 노출을 위해 `slot.getBookingClass().getId()`가 필요하다.

### 결정
컨트롤러에서 `SlotResponse.from(slot)` 호출 방식은 유지하되,
OSIV에 의존하지 않는 것을 전제로 한다.

현재 DTO가 `BookingClass` 전체가 아닌 식별자(`id`)만 참조하므로
현 구현 범위에서는 동작한다.

### 대안
- 서비스 레이어에서 DTO 변환 → Lazy 프록시 접근 위험 최소화. 대신 서비스가 HTTP 응답 포맷에 의존
- `FetchType.EAGER`로 변경 → 모든 Slot 조회에서 BookingClass도 함께 로딩. 불필요한 조인 발생
- `@EntityGraph` 사용 → 특정 쿼리에서만 eager 로딩. 구현 추가 필요

### 트레이드오프 / 위험
- `SlotResponse`에 `BookingClass`의 추가 필드(예: name/category)를 노출하기 시작하면
  컨트롤러 변환 시 `LazyInitializationException` 위험이 생긴다.
- 이 경우 서비스 레이어에서 DTO를 조립하거나,
  조회 쿼리에 `fetch join`/`@EntityGraph`를 적용해야 한다.
- OSIV 비활성화는 커넥션 장기 점유를 줄이는 대신, 조회 경계 설계를 더 엄격히 요구한다.

---

## Decision 3: `createSlot()` — 앱 레벨 중복 체크 + DB UNIQUE 이중 방어선

### 배경
DB에 `UNIQUE(class_id, start_at)` 제약이 있지만, 충돌 시 `DataIntegrityViolationException`이 발생해 클라이언트에 `500`이 반환된다.

### 결정
앱 레벨에서 `existsByBookingClassIdAndStartAt()`로 선행 검사 후, DB 제약을 최후 방어선으로 유지한다.

```
앱 레벨 체크  →  400 INVALID_INPUT  (명확한 에러 메시지)
      ↓ (TOCTOU 통과 시)
DB UNIQUE      →  DataIntegrityViolationException → 현재 미처리(500)
```

### 트레이드오프 / 위험
- **TOCTOU 경쟁 조건**: 두 요청이 동시에 앱 레벨 체크를 통과하면 DB 제약이 발동. 현재 `GlobalExceptionHandler`에 `DataIntegrityViolationException` 핸들러 없음 → `500` 반환.
- **미결 과제**: `GlobalExceptionHandler`에 `DataIntegrityViolationException` → `409 INVALID_INPUT` 변환 핸들러 추가 검토.
- 슬롯 생성 빈도가 낮아(관리자 단독 조작) TOCTOU 실제 발생 가능성은 낮음.

---

## Decision 4: 버퍼 슬롯 비활성화 — 개별 `save()` (N+1 트레이드오프 수용)

### 배경
`confirmBooking()` 내에서 버퍼 범위 슬롯을 `deactivate()` 후 각각 `save()` 호출한다.

### 결정
개별 `save()` 루프를 사용한다. 현재 `buffer_min=30`이고 슬롯 간격이 보통 30분 이상이므로 실제 비활성화 대상은 0~1개다.

### 대안
```java
// 일괄 UPDATE (최적화)
@Modifying
@Query("UPDATE Slot s SET s.isActive = false " +
       "WHERE s.bookingClass.id = :classId " +
       "AND s.startAt >= :start AND s.startAt < :end")
void deactivateInBufferWindow(...);
```

### 트레이드오프 / 위험
- 슬롯을 빽빽하게 배치(예: 10분 간격)하면 buffer_min=30 범위에 최대 2개 슬롯 → N+1 발생.
- **성능 기준 초과 시**: 위 `@Modifying @Query`로 교체. 단, `deactivate()` 도메인 메서드 호출 없이 직접 DB UPDATE → 도메인 로직 우회 주의.

---

## Consequences

**공통 위험 요약**

| 위험 | 트리거 조건 | 조치 |
|------|------------|------|
| LazyInitializationException | DTO가 LAZY 연관 필드를 추가로 참조 | 서비스 DTO 조립 또는 fetch join/@EntityGraph 적용 |
| createSlot TOCTOU 500 | 동시 슬롯 생성 | GlobalExceptionHandler에 DataIntegrityViolationException 핸들러 추가 |
| 버퍼 N+1 | 고밀도 슬롯 배치 | @Modifying 일괄 UPDATE로 교체 |

---

## References

- `docs/PRD/0001_spec/spec.md` §4.1 (슬롯 정원, 버퍼)
- ADR-0003 (비관적 락 — confirmBooking 트랜잭션 계약)
- `app/booking/SlotManagementService.java`
