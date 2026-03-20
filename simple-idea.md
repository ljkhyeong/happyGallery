# Simple Idea

간단한 개선/리팩토링 아이디어는 이 파일에 `As-Is | To-Be` 두 열 표로 한 줄씩 누적한다.
복잡한 설계 논의, 실험 결과, 회고 기록은 `docs/Idea`, `docs/POC`, `docs/Retrospective`, `docs/ADR`로 분리한다.

| As-Is | To-Be |
|------|-------|
| `AdminAuthFilter`가 URI prefix와 `/auth/` 문자열 포함 여부로 경로를 판정한다. | 공용 matcher 또는 명시적 admin route registry로 우회 가능성을 줄인다. |
| `AdminBookingResponse`는 guest 전용 이름/전화번호 전제를 강하게 가진다. | guest/member/claimed booking을 모두 담을 수 있는 `customerSummary` 형태로 단순화한다. |
| `ProductQueryService`가 상품 목록 뒤에 재고를 건별 조회한다. | 상품+재고 projection 조회 한 번으로 목록을 만들도록 바꾼다. |
| `Me*Controller` 3개가 DTO를 inner record로 정의하고 나머지는 별도 `dto/` 파일을 사용해 기준이 혼재한다. | DTO는 항상 별도 파일(`dto/` 패키지)로 분리해 위치를 예측 가능하게 유지한다. |
| ~~컨트롤러가 호출하는 일부 `UseCase`가 하나의 요청 문맥을 이루는 값들을 scalar 파라미터로 직접 나열한다.~~ | ~~controller request와 별도로 app 경계용 command DTO를 두어 유스케이스 입력을 의미 단위로 묶고 순서 의존을 줄인다.~~ |
| ~~`DefaultBookingCancelService`가 `BOOKED` 상태 확인을 직접 수행하고 `Booking.cancel()`은 단순 상태 대입만 한다.~~ | ~~취소 가능 상태 검증을 `Booking.cancel()` 안으로 옮겨 예약 취소 전이 규칙을 도메인에 응집시킨다.~~ |
| ~~`DefaultBookingCancelService`가 환불 가능 여부 판단과 8회권 크레딧 복구/예약금 환불 분기를 한 메서드에서 직접 조합한다.~~ | ~~예약 취소 후 보상 처리를 별도 helper 또는 support로 묶어 시간 경계 판단과 보상 적용 책임을 분리한다.~~ |
| ~~예약금 예약 취소 시 `refundable`만 보고 `DEPOSIT_REFUNDED` 알림을 보내 PG 환불 실패와 성공을 구분하지 않는다.~~ | ~~`RefundStatus.SUCCEEDED`를 확인한 경우에만 환불 완료 알림을 보내고, 실패는 무알림 또는 별도 이벤트로 분리한다.~~ |
| UseCase가 JPA 엔티티(`Booking`, `Slot` 등)를 컨트롤러에 직접 반환한다. 현재는 즉시 DTO 변환하므로 안전하지만 비동기 처리 도입 시 `LazyInitializationException` 위험이 있다. | 비동기 응답 조립이 필요해지는 시점에 UseCase 반환 타입을 record로 점진 전환한다. `CancelResult`, `ProductionResult` 등 기존 패턴을 따른다. |
