---
name: domain-state-machine
description: happyGallery의 상태 enum·전이 메서드·금지 상태 검사나 중복 도메인 검증을 변경할 때 사용한다. 단순 조회 분류 조건에는 적용하지 않는다.
---

# happyGallery 상태 전이

## 규칙

- 허용 전이와 사용자 의미는 PRD-0001, 책임 구분은 ADR-0002를 확인한다. 상태 구현은 `domain/src/main/java/com/personal/happygallery/domain/`에 있다.
- 전이 가능 여부와 상태 변경은 엔티티·정책 enum이 담당한다. application은 인증·트랜잭션·외부 결과·다른 엔티티와의 관계를 처리한다.
- 하나의 엔티티 필드만 검사하는 규칙이 여러 서비스에 반복되면 도메인 guard로 합친다. 조회 분류용 상태 비교는 그대로 둘 수 있다.
- enum 변경 시 switch, 영속성 쿼리, MyBatis, JSON 계약, 화면 상태 표시, 지표 label, migration을 검색한다.
- 비동기 환불 상태의 원본은 `Refund.status`다. 주문·예약·이용권에 같은 상태를 복제하지 않는다.

## 검증

- 전이·guard는 해당 `:application:policyTest`, DB 흐름은 해당 `:application:useCaseTest`를 실행한다.
- 외부에 노출되는 상태가 바뀌면 `api-contract`를 함께 적용하고 PRD·해당 ADR·화면 상태 표시를 갱신한다.
