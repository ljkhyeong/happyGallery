---
name: happygallery-booking-flows
description: happyGallery의 예약 생성·변경·취소, 슬롯 정원·잠금·버퍼, 비회원 예약 조회를 변경할 때 사용한다. 결제 실행·휴대폰 인증 자체는 해당 전용 스킬을 사용한다.
---

# happyGallery 예약·슬롯

## 규칙

- `application/src/main/java/com/personal/happygallery/application/booking/`, PRD-0001, ADR-0003~0007을 확인한다. 환불·알림 처리가 바뀌면 ADR-0018·0032도 확인한다.
- 빠른 사전 확인은 immutable projection으로 한다. 최종 정원 판단은 클래스 ID·슬롯 ID를 일정한 순서로 잠그고 현재 슬롯을 다시 읽어 수행한다. 슬롯 잠금은 `SELECT ... FOR UPDATE`를 유지한다.
- 예약과 정원 변경은 같은 트랜잭션에서 처리한다. 변경 시 수업 호환성 검사를 정원 변경보다 먼저 수행해 오류 우선순위를 유지한다.
- 정원은 8명이다. `bookedCount`는 예약 인원, `bufferBlockCount`는 겹친 차단 수, `adminActive`는 관리자 설정이다. 첫 예약에서 버퍼를 차단하고 마지막 겹친 예약이 사라질 때 해제한다.
- 환불·변경 마감은 `time-boundary-policy`를 따른다. 비회원 access token과 중복 예약 방지를 유지한다.
- 관리자 취소 후 잔금 정산·만료 이용권 보상은 예약/유형별 유일한 후속 작업으로 저장한다. 예약·슬롯·이력·환불 또는 이용권 복원·outbox와 같은 트랜잭션에서 생성한다.
- 후속 작업 완료는 행 잠금 아래 처리하고 담당자·시간을 기록한다. 반복 요청은 기존 결과와 `changed=false`를 반환하며 정산·보상을 중복 실행하지 않는다.
- 관리자 과거 조회는 탈퇴 회원을 포함하는 전용 일괄 조회를 사용한다. `userId`가 있으면 회원 예약으로 유지하고 익명화된 이름·null 전화번호만 노출한다.
- 알림 outbox는 예약 트랜잭션에서 저장하고 커밋 후 발송한다.
- 유료 예약은 서버가 수업료의 10%를 예약금으로 계산하고 결제 confirm 후 생성한다. 결제 변경은 `happygallery-payment-flows`를 함께 적용한다.

## 검증

- 변경에 맞게 `GuestBookingUseCaseIT`, `BookingRescheduleUseCaseIT`, `BookingCancelUseCaseIT`, `SlotBookingCapacityUseCaseIT`를 선택한다.
- 잠금·정원이 바뀌면 관련 동시성 시나리오를, 마감이 바뀌면 해당 policy test를 확인한다.
