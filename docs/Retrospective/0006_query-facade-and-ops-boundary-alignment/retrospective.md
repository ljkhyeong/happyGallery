# query facade와 운영 경계 정리 회고

**날짜**: 2026-03-17  
**상태**: Retrospective note

---

## 배경

`main` 히스토리에는 기능 구현 직후 controller 조합 로직이나 direct dependency를 줄이는 리팩터가 반복된다.

대표 커밋:
- `1bd103d` Controller → Repository 직접 의존 제거
- `6d381aa` OrderProductionService 내부 DTO로 컨트롤러 조합 로직 제거
- `063df3e` 주문 배치 정합성과 승인 가드 정리
- `2f2cda4` 유스케이스 포트 경계 정리

즉, 기능은 잘 추가되지만 web/query/운영 경계는 항상 뒤늦게 다시 맞춰지고 있다.

---

## 관찰

- controller가 가격 조회, 상태 필터, 응답 조합, 소유권 판정을 직접 들고 있는 경우가 남는다.
- admin query와 member query는 write use case보다 정리 우선순위가 뒤로 밀리기 쉽다.
- notification/refund/audit는 기능적으로는 잘 맞지만, 저장/재시도/운영 조회 경계가 feature별로 흩어져 있다.

---

## 회고 포인트

### 1. admin/member query facade를 명시적으로 둔다

As-Is:
- controller가 query 조합 규칙을 직접 알고 있다.

To-Be:
- `Admin*QueryService`, `Me*QueryService` 같은 facade를 두고
  controller는 HTTP 매핑과 요청 검증에 더 가깝게 유지한다.

### 2. query projection을 feature와 함께 설계한다

As-Is:
- 상품+재고, booking 고객 요약, 주문 운영 리스트 같은 조회가
  엔티티 조합이나 후처리 스트림에 의존한다.

To-Be:
- projection/read model을 먼저 두고 N+1이나 null 전제를 줄인다.

### 3. 운영성 경계는 payment/notification 단위로 다시 모은다

As-Is:
- refund, notification log, audit record가 도메인별 service에 흩어져 있다.

To-Be:
- 공용 refund boundary, notification log store boundary, 운영 read model을 정리해
  order/booking/batch가 같은 방식으로 호출하도록 맞춘다.

---

## 현재 결론

히스토리상 “기능 구현 후 query/controller 정리”는 반복되는 패턴이다.  
다음부터는 write use case와 함께 query facade와 운영 조회 경계까지 한 번에 설계하는 편이 낫다.
