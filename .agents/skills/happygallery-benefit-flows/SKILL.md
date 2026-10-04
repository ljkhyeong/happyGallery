---
name: happygallery-benefit-flows
description: happyGallery의 이벤트·쿠폰·적립금과 회원 주문 결제 혜택의 예약·사용·해제·환불 복원·회수를 변경할 때 사용한다. PG 호출·결제 시도 상태 전이는 결제 스킬, 주문·클레임 환불 정책은 주문 스킬을 사용한다.
---

# happyGallery 이벤트·쿠폰·적립금

## 규칙

- `application/src/main/java/com/personal/happygallery/application/`의 `coupon/`·`reward/`·`event/`, `payment/OrderPaymentBenefitReservationService`, PRD-0001 3.1.1, ADR-0042를 확인한다.
- 쿠폰·적립금은 회원 `ORDER`에만 적용한다. 쿠폰은 배송비를 뺀 상품 금액 기준이고 적립금 상한은 쿠폰 할인 뒤 상품 금액이다. 품목 배분은 쿠폰은 원금, 적립금은 쿠폰 뒤 금액 비율로 `ProportionalAmountAllocator`를 쓴다.
- 정률 할인은 원 미만을 버리고 결과가 0원이면 적용 불가로 거절한다. 발급 이력이 있는 정의는 이름·할인 조건·유효기간을 바꾸지 않고 활성·공개 플래그만 바꾼다. 삭제는 비활성화이며 미사용 발급분은 회원 쿠폰 조회 때 CANCELED로 정리된다.
- 쿠폰 정의는 공개 발급·prepare 견적에서 공유 잠금, 관리자 수정·비활성화에서 배타 잠금으로 직렬화한다. 발급 쿠폰은 배타 잠금으로 견적·예약·사용·해제한다. 회원별 중복 발급은 DB 유일 제약이 최종 보장한다.
- 적립금 변경은 `MemberAccountGuard`로 회원 행을 먼저 잠근 뒤 계정·lot을 잠그고 `expireLots`로 만료를 먼저 반영한다. lot은 만료가 가까운 순으로 배분한다.
- prepare는 견적·예약을 `PaymentAttempt` 저장과 같은 트랜잭션에서 처리한다. confirm은 저장된 pricing을 검증만 하고 주문 생성 트랜잭션에서 `redeem`·`consume`한다.
- 예약 해제는 attempt가 FAILED·CANCELED·COMPENSATED일 때만 허용한다. 만료·고객 종료·미승인 확정은 `payloadEnc`를 지우므로 `readPayloadForRelease`로 먼저 읽고 상태 전이 뒤 해제한다.
- PG 승인 가능성이 남은 실패와 `RECONCILIATION_REQUIRED`는 예약을 유지한다. PG 호출 전 확정 실패·미승인 확정·0원 주문 생성 실패만 해제하고, 보상 환불은 성공 확정 때만 해제한다.
- 해제 시점에 유효기간이 지난 쿠폰은 EXPIRED가 된다. 만료된 적립 배분은 반환하지 않고 RELEASE와 별도 EXPIRE 원장을 남긴다. 반환·적립·복원액은 `credit`으로 부채를 먼저 상환한다.
- 환불은 PG 취소액, 고객 반환 총액(PG+적립금 복원), 적립금 복원·회수, 쿠폰 복원 여부를 분리해 고정한다. 쿠폰 복원은 전액 주문 환불만 가능하다. PG 0원이면 외부 호출 없이 같은 성공 후처리를 탄다.
- 사용 적립금 복원은 원 lot의 주문 출처·만료를 유지하고 이미 만료면 복원 시점부터 30일을 준다. 전액 취소 때 만료된 쿠폰은 주문 참조를 보존한 EXPIRED로 둔다.
- 적립 회수 부족분은 음수 잔액 대신 부채로 남긴다. 클레임 회수액은 누적 상품 PG 환불 비율로 계산하고 이미 요청된 환불의 회수액을 뺀다.
- 원장 멱등키는 `reward:<동작>:<attempt|order|refund|lot>:<id>`이고 유일 제약으로 보장한다. 복원은 예약 행, 회수는 회원 행을 잠근 뒤 키를 다시 확인하고 중복 요청도 성공으로 끝낸다.
- 적립은 배송·픽업 완료 때 `rewardEarnBase`의 1%를 원 단위 내림해 1년 만료 lot으로 주문당 한 번 지급한다.
- 이벤트 공개 조회는 게시 상태이고 `endAt > now`인 이벤트라 예정 이벤트도 포함한다. 관리자 수정·삭제는 기대 버전을 검사하고, 관련 상품 ID 오름차순과 연결 쿠폰 `existsById` 확인을 유지한다.
- `quoteAndLock`은 행을 잠그므로 화면 입력별 조회에 쓰지 않는다. 기간 경계는 `time-boundary-policy`, 쿠폰·적립금 상태 enum은 `domain-state-machine`을 함께 적용한다.

## 검증

- 정책은 `./gradlew :application:policyTest --tests "*CouponPolicyTest"`처럼 `CouponPolicyTest`·`OrderBenefitPolicyTest`·`RefundInvariantPolicyTest`·`RewardAccrualPolicyTest`·`EventPolicyTest` 중 선택한다.
- DB 흐름은 `./gradlew --no-daemon :application:useCaseTest --tests "*X*"`로 발급·잠금 `CouponUseCaseIT`, 원장·부채·멱등 `RewardUseCaseIT`, 이벤트 `EventUseCaseIT`를 선택한다.
- 결제 연결은 `OrderPaymentBenefitUseCaseIT`, 환불은 `MixedOrderRefundUseCaseIT`·`OrderClaimUseCaseIT`를 선택한다. 해제 허용 상태·보상·클레임 배분은 `./gradlew :application:test --tests`로 `OrderPaymentBenefitReservationServiceTest`·`RefundTransactionServiceTest`·`OrderClaimRefundCalculatorTest`를 실행한다.
- API가 바뀌면 `api-contract`와 `./gradlew --no-daemon :adapter-in-web:restDocsTest --tests "*PromotionBenefitApiRestDocsTest"`, schema는 `entity-migration-sync`, 화면은 `happygallery-frontend-flows`를 함께 적용한다.
