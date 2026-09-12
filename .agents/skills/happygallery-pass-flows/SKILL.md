---
name: happygallery-pass-flows
description: happyGallery 이용권 구매·사용 횟수·만료·환불·노쇼와 이용권 예약 취소를 변경할 때 사용한다. PG 승인·환불 실행이 주목적이면 결제 스킬을 사용한다.
---

# happyGallery 이용권

## 규칙

- `application/src/main/java/com/personal/happygallery/application/pass/`, PRD-0001, ADR-0010·0011·0018을 확인한다.
- 이용권 구매는 회원만 가능하다. 도메인 factory·JPA·DB의 필수 소유권을 유지한다.
- 구매 금액은 서버 `PASS_TOTAL_PRICE`를 사용하고, 유료 이용권은 결제 confirm 후 생성한다.
- 만료는 구매일 +90일, 만료 알림은 7일 전이다. 시간 계산 변경은 `time-boundary-policy`를 함께 적용한다.
- 소유권 확인·사용 횟수 변경·원장 저장을 같은 트랜잭션에서 처리한다.
- 잔여 횟수 기준 환불과 미래 예약 자동 취소를 함께 확인한다. 취소·노쇼가 횟수를 중복 차감하거나 복원하지 않아야 한다.

## 검증

- 변경에 맞게 `PassPurchaseUseCaseIT`, `PassCreditUsageUseCaseIT`, `PassExpiryNotificationUseCaseIT`를 선택한다.
- 예약 취소·노쇼 처리도 바뀔 때 해당 예약 테스트를 추가한다.
