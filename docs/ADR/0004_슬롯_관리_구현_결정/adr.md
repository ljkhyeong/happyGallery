# ADR-0004: 슬롯 관리 구현 수준 설계 선택

- **상태**: 확정
- **날짜**: 2026-02-22
- **관련 파일**:
  - `application/build.gradle`
  - `application/src/main/java/com/personal/happygallery/application/booking/DefaultSlotManagementService.java`
  - `application/src/main/java/com/personal/happygallery/application/booking/BookingCalendarSlotMaterializer.java`
  - `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/dto/SlotResponse.java`
  - `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/SlotRepository.java`

---

## 배경

§5.1 슬롯 관리 구현 중 아키텍처 결정(ADR-0003) 외에도,
구현 레벨에서 여러 선택 지점이 있었다.
이 ADR은 각 선택의 이유와 트레이드오프를 기록한다.

---

## Decision 1: `application` 조회 port는 영속성 pagination 타입을 노출하지 않는다

### 배경
`application` 모듈에서 `@Transactional`을 사용하려면 `spring-tx`가 필요하다.
과거 일부 outbound port는 `Pageable`을 공개 시그니처로 사용해 application service가
`PageRequest`를 만들었지만, 조회 의도가 이미 메서드 이름과 커서·제한 수로 고정된 경계에
Spring Data pagination 타입을 노출할 이유는 없다.

### 결정
`application` 조회 port는 `limit`과 필요한 커서·필터만 받는다.
Spring Data repository 또는 persistence adapter가 이 값을 `PageRequest`로 변환한다.

- `spring-tx`, `spring-orm`: 트랜잭션과 낙관 락 예외 처리 구현에 필요한 `implementation`으로 유지
- `spring-data-commons`: application 공개 API에서 제거
- JPA 스타터와 `PageRequest` 생성: `adapter-out-persistence` 구현 책임으로 유지

### 대안
- port에서 `Pageable` 유지 → adapter 코드는 짧지만 application 경계가 Spring Data에 결합된다.
- 별도 범용 pagination value object 추가 → 현재 조회는 정렬과 방향이 메서드 의도로 고정돼 있어 불필요한 추상화다.

### 트레이드오프 / 위험
- 새로운 조회가 임의 정렬이나 양방향 페이지 이동을 실제로 요구하면 application 의미에 맞는
  요청 모델을 별도로 설계한다. persistence의 `Pageable` 자체를 다시 노출하지 않는다.
- JPA 관련 설정(`spring.jpa.*`)은 `bootstrap` 모듈의 `application.yml`에서 관리한다.

### 업데이트

- 2026-06-22: `java-library`를 적용하고, 공개 API에 드러나는 의존성은 `api`, 구현 전용 의존성은 `implementation`으로 정리했다.
- 2026-07-30: 고정 의도 조회 port를 `limit`·커서 기반으로 바꾸고 Spring Data pagination 타입을 persistence adapter 내부로 이동했다.
- 2026-08-08: 회원 주문·예약·8회권·문의, 공개/작성자 Q&A, 관리자 상품별 Q&A와 비회원 복구 이력도 같은 `CursorPage`·`CursorUtils` 경계를 사용한다. 기존 `/api/v1` 배열 응답은 최신 100건으로 제한해 호환을 유지하고, 신규 `/page` 응답이 `content`·`nextCursor`·`hasMore`를 제공한다. repository만 `PageRequest.ofSize(limit)`를 사용한다.

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

## Decision 3: 자동 회차 구체화 — 클래스 잠금 + DB 유일 제약

### 배경
공개 캘린더 조회가 필요한 날짜 범위의 회차를 자동으로 구체화하므로 관리자 요청으로 회차를 따로 만들지 않는다.
동시에 같은 클래스와 날짜를 조회해도 동일 시작 시각의 슬롯 행은 하나만 존재해야 하며, 종료 시각과 버퍼 상태는
항상 현재 클래스 정책으로 계산해야 한다.

### 결정
- 공개 회차 조회는 `classes` 행을 먼저 잠그고 조회 범위의 기존 시작 시각을 한 번 읽는다.
- 캘린더 규칙에 필요하지만 없는 시작 시각만 `Slot`으로 만들며, `endAt`은 클래스의 `durationMin`으로 계산하고 `capacity`는 클래스 등록값을 복사한다.
- 기존 예약과 수업·정리 구간이 겹치는 새 회차는 생성 시점부터 버퍼 차단 수를 반영한다.
- `(class_id, start_at)` DB 유일 제약은 애플리케이션 밖의 쓰기와 잠금 경계 누락에 대한 최후 방어선으로 유지한다.
- 관리자 단건·일괄 생성 API와 애플리케이션 선행 중복 조회는 제거한다.

### 트레이드오프 / 위험
- 같은 클래스의 자동 회차 구체화와 예약·반납은 짧은 클래스 행 잠금 경계에서 직렬화된다.
- 클래스별로만 직렬화하므로 서로 다른 클래스 작업은 병렬 처리된다.
- DB를 애플리케이션 밖에서 직접 변경하는 경우에는 UNIQUE 제약이 최후 방어선으로 남는다.

---

## Decision 4: 충돌 슬롯 차단 수 갱신 — 도메인 변경 후 `saveAll()`

### 배경
`SlotCapacitySupport`는 원인 슬롯의 첫 예약에서 수업·정리 구간이 겹치는 슬롯의 `bufferBlockCount`를 증가시키고,
마지막 예약 반납에서 감소시킨 뒤 저장한다.

### 결정
잠긴 충돌 슬롯은 도메인 메서드로 차단 수를 변경한 뒤 `saveAll()` 한 번으로 저장한다. 자동 캘린더의 시작 간격이
클래스 소요 시간보다 짧아 한 예약이 여러 후보 회차를 막을 수 있으므로 개별 저장 호출을 반복하지 않는다.

### 트레이드오프 / 위험
- 각 엔티티 변경은 Hibernate dirty checking과 배치 설정을 사용한다. 실제 SQL 배치가 성능 기준을 넘으면 원자적
  `@Modifying` 쿼리를 검토하되, 0 미만 방지 조건을 DB 쿼리에도 유지한다.

---

## Decision 5: 공개 예약 가능 시각과 최종 확정 시각을 이중 검증

### 배경

날짜별 공개 조회가 활성 상태와 잔여 정원만 확인하면 이미 시작한 슬롯도 노출된다. 또한 조회 당시에는
미래 슬롯이었더라도 결제·확정 전에 시작 시각이 지날 수 있어 조회 필터만으로는 예약 정합성을 보장할 수 없다.

### 결정

- 공개 슬롯 조회는 주입된 `Clock`의 현재 시각보다 뒤에 시작하는 슬롯만 반환한다.
- 시작 시각과 현재 시각이 같으면 이미 시작한 슬롯으로 보고 제외한다.
- `SlotCapacitySupport.reserveCapacity()`는 원인 슬롯을 비관적으로 잠근 뒤 같은 시간 규칙을 다시 검증한다.

### 결과

공개 화면은 예약 가능한 슬롯만 보여 주고, 조회와 최종 확정 사이에 시간이 경과해도 잠금 트랜잭션에서
예약을 거절한다. 시간 기준은 운영체제 기본 시각이 아니라 애플리케이션의 Asia/Seoul `Clock`을 따른다.

---

## Decision 6: 관리자 활성 상태는 양방향으로 변경한다

### 배경

`admin_active=false`는 자동 버퍼 차단과 별개의 운영자 판단이지만, 비활성화만 가능하면 실수로 끈 슬롯을
DB에서 직접 수정해야 한다.

### 결정

- `PATCH /api/v1/admin/slots/{id}/activate`로 `admin_active=true`를 복구한다.
- 활성화와 비활성화 모두 슬롯 행을 잠근 같은 관리 유스케이스를 사용한다.
- 활성화는 `buffer_block_count`를 변경하지 않는다. 따라서 버퍼 차단 중인 슬롯은 `adminActive=true`여도
  `isActive=false`를 유지한다.

---

## Decision 7: 회차 취소는 슬롯 비활성화와 예약 취소를 분리한다

### 배경

운영자 사정으로 수업 한 회차를 취소할 때 예약만 순차 취소하면 처리 중 새 예약이 들어올 수 있다. 반대로 슬롯 비활성화만으로는 기존 예약의 예약금 환불, 8회권 복구와 고객 알림이 실행되지 않는다.

### 결정

- 운영자는 슬롯을 먼저 관리자 비활성화해 신규 예약을 차단한다.
- 회차 일괄 취소 API는 `admin_active=false`인 슬롯만 받으며, 그 슬롯의 `BOOKED` 예약을 개별 관리자 취소 정책과 같은 경계로 처리한다.
- 대상 8회권 ID만 먼저 조회해 ID 오름차순으로 이용권 행을 잠그고, 클래스와 슬롯을 잠근 뒤 `BOOKED` 예약 행을 현재 읽기(`FOR UPDATE`)로 다시 조회한다. 기존 `pass_purchases -> classes -> slots -> bookings` 순서를 유지하면서 잠금 전 예약 스냅샷을 처리하지 않는다.
- 응답은 취소 건수, 크레딧 복구 건수, 예약금 환불 요청 건수와 수동 정산 필요 건수를 구분한다. PG 환불 완료 건수로 해석하지 않는다.

### 결과

슬롯 비활성화는 판매 중단, 예약 취소는 고객 보상이라는 서로 다른 의미를 유지한다. 처리 중 신규 예약을 막고, 재시도 때 이미 취소된 예약을 다시 보상하지 않는다.

---

## Decision 8: 운영 캘린더는 기본 개방하고 슬롯은 조회 시 자동 구체화한다

### 배경

관리자가 클래스마다 가능한 슬롯을 단건 또는 기간·요일 조합으로 계속 생성하는 방식은 휴무보다 정상 영업일이
많은 공방에서 입력량이 크다. 반면 결제·변경·취소는 안정적인 슬롯 ID와 행 잠금이 필요해 슬롯 엔티티 자체를
없앨 수 없다.

### 결정

- 단일 `booking_calendar_settings`에서 기본 운영시간, 시작 간격, 법정 공휴일 차단 여부를 관리한다.
- `booking_day_overrides`는 날짜별 `OPEN|CLOSED`, `booking_time_blocks`는 날짜 안의 차단 시간을 저장한다.
- 공개 조회는 클래스 행을 먼저 잠그고 캘린더 규칙에 필요한 `slots`를 생성한다. 기존 슬롯은
  `calendar_active`만 갱신하며 예약·관리자 비활성 상태를 지우지 않는다.
- 공휴일 음력 날짜는 한국천문연구원 기준 `KoreanLunarCalendar`로 변환하고, 대체공휴일은 2026년 시행 중인
  `관공서의 공휴일에 관한 규정`을 적용한다. 임시공휴일과 선거일은 날짜 차단으로 보완한다.
- 자동 시작 간격은 클래스 길이보다 짧을 수 있으므로 각 슬롯의 `[startAt, endAt + bufferMin)`이 겹치는지를
  양방향으로 판정한다.
- 단건·일괄 슬롯 생성 API는 제거하고 공개 캘린더 조회를 유일한 회차 생성 경로로 사용한다.

### 결과

관리자는 정상 영업일을 반복 입력하지 않고 예외만 닫는다. 기존 슬롯 ID 기반 결제·취소 계약과
`classes → slots` 잠금 순서를 유지하면서 캘린더 변경과 예약 확정을 직렬화한다.

---

## 결과

**공통 위험 요약**

| 위험 | 트리거 조건 | 조치 |
|------|------------|------|
| LazyInitializationException | DTO가 LAZY 연관 필드를 추가로 참조 | 서비스 DTO 조립 또는 fetch join/@EntityGraph 적용 |
| 클래스 단위 잠금 경합 | 같은 클래스에 회차 조회·예약이 집중 | 트랜잭션을 짧게 유지하고 잠금 대기 지표 확인 |
| 고밀도 회차 차단 갱신 | 짧은 시작 간격과 긴 클래스 | 저장 배치 지표를 보고 원자적 일괄 UPDATE 검토 |

---

## References

- `docs/PRD/0001_기준_스펙/spec.md` §4.1 (슬롯 정원, 버퍼)
- ADR-0003 (비관적 락 — `reserveCapacity()` 트랜잭션 계약)
- `application/src/main/java/com/personal/happygallery/application/booking/DefaultSlotManagementService.java`
