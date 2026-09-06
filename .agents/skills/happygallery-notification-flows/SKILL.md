---
name: happygallery-notification-flows
description: happyGallery의 알림 outbox·카카오/SMS 발송·인증 SMS 전송·fallback·실패 분류·알림 이력을 변경할 때 사용한다. 발송 시점 정책은 도메인 스킬, 휴대폰 소유 확인은 identity 스킬을 사용한다.
---

# happyGallery 알림 전달

## 규칙

- `application/src/main/java/com/personal/happygallery/application/notification/`, 외부 adapter의 `notification/`, ADR-0032를 확인한다.
- 도메인 트랜잭션의 이벤트를 동기 listener가 받아 outbox에 저장한다. 커밋 후 `notificationExecutor`가 트랜잭션 없는 dispatcher를 실행하고, 짧은 트랜잭션으로 항목 claim·결과 저장을 처리한다.
- 제한된 polling 횟수 안에서 한 항목씩 claim·발송·완료한다. 순차 외부 호출 전에 여러 행을 한꺼번에 claim하지 않는다.
- 카카오 우선·SMS fallback과 성공·실패 알림 이력을 유지한다. 수신자는 평문 전화번호 대신 ID로 저장하고 발송 시 조회·복호화한다. 수신자 없음은 SYSTEM/FAILED로 끝낸다.
- 전송 전 실패만 retry·fallback한다. DNS·라우팅·TCP 연결·pool 획득·TLS handshake/인증 실패와 NHN `-9999`·`-2021`은 전송 전 일시 실패다. 응답 read timeout처럼 결과를 모르면 재발송하지 않는다.
- 발송 결과 미확인과 이력 저장 실패가 함께 발생하면 두 사실을 최종 실패 이유·지표에 남긴다. 재시도 가능한 이력 오류로 바꾸지 않는다.
- Alimtalk·일반 SMS·인증 SMS는 각각 executor와 CircuitBreaker로 격리한다. 공용 생성 규칙은 결제 스킬의 [외부 호출 제한](../happygallery-payment-flows/references/resilience.md)을 따른다.
- 인증 SMS는 `PhoneVerificationSender`로 분리한다. 소유 확인은 identity 스킬을 적용하고, 배치 알림의 중복 방지 이력을 유지한다.

## 검증

- outbox는 `NotificationOutboxUseCaseIT`, fallback·이력은 `NotificationServiceTest`, 외부 요청 형식은 `NotificationSenderContractTest`에서 영향받는 시나리오를 선택한다.
