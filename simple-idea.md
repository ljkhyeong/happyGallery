# Simple Idea

간단한 개선/리팩토링 아이디어는 이 파일에 `As-Is | To-Be` 두 열 표로 한 줄씩 누적한다.
복잡한 설계 논의, 실험 결과, 회고 기록은 `docs/Idea`, `docs/POC`, `docs/Retrospective`, `docs/ADR`로 분리한다.

| As-Is | To-Be |
|------|-------|
| `AdminAuthFilter`가 URI prefix와 `/auth/` 문자열 포함 여부로 경로를 판정한다. | 공용 matcher 또는 명시적 admin route registry로 우회 가능성을 줄인다. |
| `AdminBookingResponse`는 guest 전용 이름/전화번호 전제를 강하게 가진다. | guest/member/claimed booking을 모두 담을 수 있는 `customerSummary` 형태로 단순화한다. |
| `ProductQueryService`가 상품 목록 뒤에 재고를 건별 조회한다. | 상품+재고 projection 조회 한 번으로 목록을 만들도록 바꾼다. |
| `Me*Controller` 3개가 DTO를 inner record로 정의하고 나머지는 별도 `dto/` 파일을 사용해 기준이 혼재한다. | DTO는 항상 별도 파일(`dto/` 패키지)로 분리해 위치를 예측 가능하게 유지한다. |
