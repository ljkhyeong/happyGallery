# Simple Idea

간단한 개선/리팩토링 아이디어는 이 파일에 `As-Is | To-Be` 두 열 표로 한 줄씩 누적한다.
복잡한 설계 논의, 실험 결과, 회고 기록은 `docs/Idea`, `docs/POC`, `docs/Retrospective`, `docs/ADR`로 분리한다.

| As-Is | To-Be |
|------|-------|
| `Order`가 guest/member와 token 유무에 따라 여러 생성자를 직접 노출한다. | `Order.forGuest(...)`, `Order.forMember(...)` 같은 명명된 팩토리 메서드로 생성 의도를 드러낸다. |
| 비회원 주문/예약 조회가 `id + token` query param 조합을 그대로 사용한다. | query param 대신 header/body 또는 짧은 만료의 signed token 방식으로 정리한다. |
| `AdminAuthFilter`가 URI prefix와 `/auth/` 문자열 포함 여부로 경로를 판정한다. | 공용 matcher 또는 명시적 admin route registry로 우회 가능성을 줄인다. |
| `AdminBookingResponse`는 guest 전용 이름/전화번호 전제를 강하게 가진다. | guest/member/claimed booking을 모두 담을 수 있는 `customerSummary` 형태로 단순화한다. |
| `ProductQueryService`가 상품 목록 뒤에 재고를 건별 조회한다. | 상품+재고 projection 조회 한 번으로 목록을 만들도록 바꾼다. |
| `NotificationService`가 로그 저장을 직접 repository로 처리한다. | `NotificationLogStorePort` 같은 저장 경계를 두고 알림 로직과 persistence를 분리한다. |
| local 전용 지원 기능이 여러 controller/service에 흩어져 있다. | `local-support` 성격의 묶음과 명명 규칙으로 정리해 운영 코드와 구분을 더 분명히 한다. |
| 라우트/문서 canonical 기준이 뒤늦게 정리되어 README/PRD/E2E 동기화 커밋이 반복된다. | 새 공개 경로를 도입할 때부터 canonical route와 alias 종료 시점을 함께 기록한다. |
| controller가 가격 조회, DTO 조립, 소유권 판정을 직접 들고 있는 경우가 있다. | 읽기/쓰기 use case 또는 query facade로 옮겨 controller를 HTTP 매핑에 가깝게 유지한다. |
| 기능 완료 뒤에 관측성/메트릭/문서가 따라붙는 경우가 반복됐다. | 신규 핵심 흐름에는 최소 문서와 최소 운영 지표를 feature 정의 단계에서 같이 적는다. |
| `app.*` 설정이 개별 필드나 문자열 key 수준으로 흩어질 수 있다. | `AdminProperties`, `RateLimitProperties`, `BatchSchedulerProperties`처럼 concern별 `@ConfigurationProperties` 클래스로 묶고 기본값/검증을 같이 둔다. |
