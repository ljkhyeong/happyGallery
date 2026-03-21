# Simple Idea

간단한 개선/리팩토링 아이디어는 이 파일에 `As-Is | To-Be` 두 열 표로 한 줄씩 누적한다.
복잡한 설계 논의, 실험 결과, 회고 기록은 `docs/Idea`, `docs/POC`, `docs/Retrospective`, `docs/ADR`로 분리한다.

| As-Is | To-Be |
|------|-------|
| `AdminAuthFilter`가 URL 시작 문자열과 `/auth/` 포함 여부만 보고 관리자 경로를 판정한다. | 공통 경로 판정 규칙이나 명시적 관리자 경로 목록을 두어 우회 가능성을 줄인다. |
| `AdminBookingResponse`는 비회원 예약 기준의 이름/전화번호 구조를 강하게 전제한다. | 비회원 예약, 회원 예약, 이관된 예약을 모두 담을 수 있는 `customerSummary` 구조로 단순화한다. |
| ~~`ProductQueryService`가 상품 목록 뒤에 재고를 건별 조회한다.~~ | ~~상품과 재고를 함께 읽는 조회 한 번으로 목록을 만들도록 바꾼다.~~ |
| ~~`Me*Controller` 3개가 DTO를 내부 record로 정의하고, 나머지는 별도 `dto/` 파일을 사용해 기준이 섞여 있다.~~ | ~~DTO는 항상 별도 파일(`dto/` 패키지)로 분리해 위치를 예측 가능하게 유지한다.~~ |
| `@UseCaseIT` 일부가 실제 동작을 호출해 검증하는 부분에서도 구현 클래스(`Default*Service`, `*Service`)를 직접 주입한다. 그래서 웹 요청 검증과 서비스 직접 호출 검증이 한 테스트 안에 섞여 있다. | 웹으로 들어오는 기능은 `MockMvc` 또는 유스케이스 인터페이스를 통해 호출하도록 맞춘다. 서비스 직접 주입은 테스트 데이터를 준비하거나 공통 준비 코드를 재사용할 때만 남긴다. |
| ~~컨트롤러가 호출하는 일부 `UseCase`가 하나의 요청 문맥을 이루는 값들을 낱개 인자로 직접 나열한다.~~ | ~~controller request와 별도로 앱 경계용 command DTO를 두어 유스케이스 입력을 의미 단위로 묶고 순서 의존을 줄인다.~~ |
| ~~주문 환불 흐름이 `Refund` 결과를 확인하지 않고 `ORDER_REFUNDED` 알림을 보낼 수 있다.~~ | ~~환불 완료 알림은 `RefundStatus.SUCCEEDED`일 때만 발송하고, 실패는 재시도 대상만 남긴다.~~ |
| ~~`Booking.reschedule()`와 `Booking.markNoShow()`의 상태 전이 검증이 서비스 레이어에 흩어져 있다.~~ | ~~예약 상태 전이 가능 여부를 도메인 메서드 안으로 옮겨 규칙을 한 곳에 응집시킨다.~~ |
| ~~`DefaultBookingCancelService`가 `BOOKED` 상태 확인을 직접 수행하고 `Booking.cancel()`은 단순 상태 대입만 한다.~~ | ~~취소 가능 상태 검증을 `Booking.cancel()` 안으로 옮겨 예약 취소 전이 규칙을 도메인에 응집시킨다.~~ |
| ~~`DefaultBookingCancelService`가 환불 가능 여부 판단과 8회권 크레딧 복구/예약금 환불 분기를 한 메서드에서 직접 조합한다.~~ | ~~예약 취소 후 보상 처리를 별도 준비 로직이나 전용 도우미로 묶어 시간 경계 판단과 보상 적용 책임을 분리한다.~~ |
| ~~예약금 예약 취소 시 `refundable`만 보고 `DEPOSIT_REFUNDED` 알림을 보내 PG 환불 실패와 성공을 구분하지 않는다.~~ | ~~`RefundStatus.SUCCEEDED`를 확인한 경우에만 환불 완료 알림을 보내고, 실패는 무알림 또는 별도 이벤트로 분리한다.~~ |
| UseCase가 JPA 엔티티(`Booking`, `Slot` 등)를 컨트롤러에 직접 반환한다. 현재는 즉시 DTO로 바꾸기 때문에 안전하지만, 비동기 처리 도입 시 `LazyInitializationException` 위험이 있다. | 비동기 응답 조립이 필요해지면 UseCase 반환 타입을 record로 바꾼다. `CancelResult`, `ProductionResult` 같은 기존 패턴을 따른다. |
| Response DTO·UseCase record의 팩토리 메서드가 ID 값을 낱개 인자로 받는 곳이 섞여 있다. 필드 추가 시 컴파일 에러로 잡히지 않고 파라미터 순서 실수 여지가 있다. | 팩토리 메서드는 엔티티 인스턴스를 직접 받도록 통일한다. Response DTO(웹 어댑터)와 UseCase record(app 레이어) 모두 도메인을 알 수 있는 의존 방향이므로 문제없다. |
