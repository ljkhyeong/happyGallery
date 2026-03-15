# MeController 리팩토링 + GuestClaimService N+1 수정

코드 품질 감사(B등급, 80/100)에서 도출된 Critical/Important 개선 작업이다.
- `MeController`가 Repository 6개를 직접 주입해 Service 레이어를 우회하고 있다.
- `GuestClaimService.claim()` 루프 내 `findById()` 반복으로 N+1 쿼리가 발생한다.
- 단일 파일에 14개 핸들러 + 12개 DTO가 집중되어 있어 도메인별 컨트롤러로 분리가 필요하다.

---

## W1. OrderQueryService — 회원 조회 메서드 추가

### 목표

`MeController`가 `OrderRepository` 등 인프라 빈을 직접 호출하는 부분을 `OrderQueryService`로 위임한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/order/OrderQueryService.java`

1. `listMyOrders(Long userId)` 추가 — `findByUserIdOrderByCreatedAtDesc()` 위임
2. `findMyOrder(Long id, Long userId)` 추가 — userId 소유권 검증 + `OrderDetail` 반환
   - `OrderDetail` record와 `OrderItemRepository`·`FulfillmentRepository` 의존성은 이미 존재하므로 재사용

### 완료 조건

- `MeController`(혹은 분리될 `MeOrderController`)가 `OrderRepository`를 직접 참조하지 않는다.

### 최소 검증

- `./gradlew build`

---

## W2. BookingQueryService — 회원 조회 메서드 추가

### 목표

`MeController`가 `BookingRepository`를 직접 호출하는 부분을 `BookingQueryService`로 위임한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/booking/BookingQueryService.java`

1. `BookingRepository` 의존성 추가 (현재는 `BookingSupport`만 주입됨)
2. `listMyBookings(Long userId)` 추가 — `findByUserIdWithDetails()` 위임
3. `findMyBooking(Long id, Long userId)` 추가 — `findByIdAndUserIdWithDetails()` + NotFoundException

### 완료 조건

- `MeController`(혹은 분리될 `MeBookingController`)가 `BookingRepository`를 직접 참조하지 않는다.

### 최소 검증

- `./gradlew build`

---

## W3. PassQueryService — 신규 생성

### 목표

8회권 도메인에 QueryService 패턴을 도입한다. `MeController`가 `PassPurchaseRepository`를 직접 호출하는 부분을 위임한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/pass/PassQueryService.java` (신규)

1. `@Service @Transactional(readOnly = true)` 클래스 생성
2. `listMyPasses(Long userId)` — `findByUserIdOrderByPurchasedAtDesc()` 위임
3. `findMyPass(Long id, Long userId)` — `findById()` + userId 소유권 검증 + NotFoundException

### 완료 조건

- `MeController`(혹은 분리될 `MePassController`)가 `PassPurchaseRepository`를 직접 참조하지 않는다.

### 최소 검증

- `./gradlew build`

---

## W4. MeOrderController — 신규 생성

### 목표

`MeController`의 주문 핸들러 3개를 별도 컨트롤러로 분리한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/web/customer/MeOrderController.java` (신규)

1. `GET /api/v1/me/orders` → `OrderQueryService.listMyOrders()` + `MyOrderSummary` 변환
2. `GET /api/v1/me/orders/{id}` → `OrderQueryService.findMyOrder()` + `OrderDetailResponse.from()`
3. `POST /api/v1/me/orders` → `ProductQueryService.getProduct()` 가격 조회 + `OrderService.createMemberOrder()`
   - 기존 `productRepository.findById()` 직접 호출 → `ProductQueryService.getProduct(id).product()` 교체
4. DTO 이동: `MyOrderSummary`, `CreateMemberOrderRequest`, `OrderItemDto`

### 완료 조건

- `GET/POST /api/v1/me/orders`, `GET /api/v1/me/orders/{id}` 동일하게 동작한다.
- `MeOrderController` 생성자에 Repository 빈이 없다.

### 최소 검증

- `./gradlew build`

---

## W5. MeBookingController — 신규 생성

### 목표

`MeController`의 예약 핸들러 5개를 별도 컨트롤러로 분리한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/web/customer/MeBookingController.java` (신규)

1. `GET /api/v1/me/bookings` → `BookingQueryService.listMyBookings()`
2. `GET /api/v1/me/bookings/{id}` → `BookingQueryService.findMyBooking()`
3. `POST /api/v1/me/bookings` → `MemberBookingService.createMemberBooking()`
4. `PATCH /api/v1/me/bookings/{id}/reschedule` → `BookingRescheduleService.rescheduleMemberBooking()`
5. `DELETE /api/v1/me/bookings/{id}` → `BookingCancelService.cancelMemberBooking()`
6. DTO 이동: `MyBookingSummary`, `MyBookingDetail`, `CreateMemberBookingRequest`, `MemberRescheduleRequest`

### 완료 조건

- 예약 5개 엔드포인트 동일하게 동작한다.
- `MeBookingController` 생성자에 Repository 빈이 없다.

### 최소 검증

- `./gradlew build`

---

## W6. MePassController — 신규 생성

### 목표

`MeController`의 8회권 핸들러 3개를 별도 컨트롤러로 분리한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/web/customer/MePassController.java` (신규)

1. `GET /api/v1/me/passes` → `PassQueryService.listMyPasses()`
2. `GET /api/v1/me/passes/{id}` → `PassQueryService.findMyPass()`
3. `POST /api/v1/me/passes` → `PassPurchaseService.purchaseForMember()`
4. DTO 이동: `MyPassSummary`, `PurchaseMemberPassRequest`

### 완료 조건

- 8회권 3개 엔드포인트 동일하게 동작한다.
- `MePassController` 생성자에 Repository 빈이 없다.

### 최소 검증

- `./gradlew build`

---

## W7. MeGuestClaimController — 신규 생성

### 목표

`MeController`의 guest claim 핸들러 3개를 별도 컨트롤러로 분리한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/web/customer/MeGuestClaimController.java` (신규)

1. `GET /api/v1/me/guest-claims/preview` → `GuestClaimService.preview()`
2. `POST /api/v1/me/guest-claims/verify` → `GuestClaimService.verifyPhoneAndPreview()`
3. `POST /api/v1/me/guest-claims` → `GuestClaimService.claim()`
4. DTO 이동: `VerifyGuestClaimPhoneRequest`, `ClaimGuestRecordsRequest`

### 완료 조건

- guest claim 3개 엔드포인트 동일하게 동작한다.

### 최소 검증

- `./gradlew build`

---

## W8. MeController — 삭제

### 목표

W4~W7 완료 후 원본 `MeController.java`를 삭제한다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/web/customer/MeController.java` 삭제

### 완료 조건

- `MeController.java` 파일이 없고 빌드가 성공한다.

### 최소 검증

- `./gradlew build`

---

## W9. GuestClaimService — claim() N+1 수정

### 목표

루프 내 `findById()` 개별 호출을 `findAllById()` 일괄 조회로 전환해 쿼리 수를 N → 1로 줄인다.

### 작업 항목

파일: `app/src/main/java/com/personal/happygallery/app/web/customer/GuestClaimService.java`

1. 주문 루프: `orderRepository.findAllById(ids)` → `Map<Long, Order>` 빌드 후 소유권 검증
2. 예약 루프: `bookingRepository.findAllById(ids)` → `Map<Long, Booking>` 빌드 후 소유권 검증
3. 8회권 루프: `passPurchaseRepository.findAllById(ids)` → `Map<Long, PassPurchase>` 빌드 후 소유권 검증
   - `findAllById()`는 `JpaRepository` 기본 제공 — Repository 수정 불필요

### 완료 조건

- `claim()` 호출 시 주문/예약/패스 각 1회 쿼리만 발생한다.
- 소유권 검증 실패 시 기존과 동일하게 `NotFoundException`을 던진다.

### 최소 검증

- `./gradlew build`

---

## 권장 순서

```
W1 · W2 · W3  (병렬 가능 — QueryService 준비)
  ↓
W4 · W5 · W6 · W7  (병렬 가능 — 컨트롤러 분리)
  ↓
W8  (MeController 삭제)

W9  (독립 — 언제든 병렬 실행 가능)
```
