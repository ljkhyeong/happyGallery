# Simple Idea

간단한 개선/리팩토링 아이디어는 이 파일에 `As-Is | To-Be` 두 열 표로 한 줄씩 누적한다.
복잡한 설계 논의, 실험 결과, 회고 기록은 `docs/Idea`, `docs/POC`, `docs/Retrospective`, `docs/ADR`로 분리한다.

| As-Is | To-Be |
|------|-------|
| `Order`가 guest/member와 token 유무에 따라 여러 생성자를 직접 노출한다. | `Order.forGuest(...)`, `Order.forMember(...)` 같은 명명된 팩토리 메서드로 생성 의도를 드러낸다. |
| `Booking`, `PassPurchase`, `Refund`도 생성 경로(게스트/회원, 예약금/이용권 등)별로 public 생성자를 오버로딩한다. | `forGuestDeposit()` / `forMemberPass()` 처럼 경로 이름을 담은 팩토리 메서드로 교체하고 단일 private 생성자에 공통 초기화를 집중한다. |
| 비회원 주문/예약 조회가 `id + token` query param 조합을 그대로 사용한다. | query param 대신 header/body 또는 짧은 만료의 signed token 방식으로 정리한다. |
| `AdminAuthFilter`가 URI prefix와 `/auth/` 문자열 포함 여부로 경로를 판정한다. | 공용 matcher 또는 명시적 admin route registry로 우회 가능성을 줄인다. |
| `AdminBookingResponse`는 guest 전용 이름/전화번호 전제를 강하게 가진다. | guest/member/claimed booking을 모두 담을 수 있는 `customerSummary` 형태로 단순화한다. |
| `ProductQueryService`가 상품 목록 뒤에 재고를 건별 조회한다. | 상품+재고 projection 조회 한 번으로 목록을 만들도록 바꾼다. |
| `NotificationService`가 로그 저장을 직접 repository로 처리한다. | `NotificationLogStorePort` 같은 저장 경계를 두고 알림 로직과 persistence를 분리한다. |
| `Me*Controller` 3개가 DTO를 inner record로 정의하고 나머지는 별도 `dto/` 파일을 사용해 기준이 혼재한다. | DTO는 항상 별도 파일(`dto/` 패키지)로 분리해 위치를 예측 가능하게 유지한다. |
| local 전용 지원 기능이 여러 controller/service에 흩어져 있다. | `local-support` 성격의 묶음과 명명 규칙으로 정리해 운영 코드와 구분을 더 분명히 한다. |
| 라우트/문서 canonical 기준이 뒤늦게 정리되어 README/PRD/E2E 동기화 커밋이 반복된다. | 새 공개 경로를 도입할 때부터 canonical route와 alias 종료 시점을 함께 기록한다. |
| controller가 가격 조회, DTO 조립, 소유권 판정을 직접 들고 있는 경우가 있다. | 읽기/쓰기 use case 또는 query facade로 옮겨 controller를 HTTP 매핑에 가깝게 유지한다. |
| 기능 완료 뒤에 관측성/메트릭/문서가 따라붙는 경우가 반복됐다. | 신규 핵심 흐름에는 최소 문서와 최소 운영 지표를 feature 정의 단계에서 같이 적는다. |
| `app.*` 설정이 개별 필드나 문자열 key 수준으로 흩어질 수 있다. | `AdminProperties`, `RateLimitProperties`, `BatchSchedulerProperties`처럼 concern별 `@ConfigurationProperties` 클래스로 묶고 기본값/검증을 같이 둔다. |
| `PasswordEncoder`가 서비스 4곳에서 `new BCryptPasswordEncoder()`로 직접 생성된다. | `PasswordEncoderConfig`에서 빈으로 등록하고 생성자 주입으로 전환해 strength 설정을 한 곳에 모은다. |
| `AdminProperties`, `RateLimitProperties`, `BatchSchedulerProperties`가 JavaBeans 바인딩(setter)으로 구현되어 가변 상태를 허용한다. | `record` + `@DefaultValue`로 전환해 불변 객체로 만들고, `@ConfigurationPropertiesScan`으로 등록한다. |
| 서비스 내부 result record(`ShippingResult`, `ProductionResult`, `PickupResult`)가 도메인 객체 필드를 반복 추출하는 `new Result(order.getId(), order.getStatus(), ...)` 패턴을 사용한다. | `Result.of(order, fulfillment)` 팩토리 메서드로 추출 로직을 record 안에 응집시킨다. (완료) |
| 일부 컨트롤러(`CustomerAuthController`, `AdminLoginController`, `AdminRefundController`)가 항상 동일 status를 반환하면서 `ResponseEntity`로 감싸고 있다. | 고정 status는 `@ResponseStatus` + DTO/void 직접 반환으로 바꾸고, `ResponseEntity`는 조건부 분기(200/404 등)에만 사용한다. (완료) |
