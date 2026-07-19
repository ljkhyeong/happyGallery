# HANDOFF

## 진행 중: 결제 후속과 신원 경로 완성

**기준일:** 2026-07-19
**활성 계획:** `plan.md`

### 현재 상태

- Toss prepare/confirm, 전 도메인 가격 스냅샷, confirm 트랜잭션 분리·멱등성·보상 환불은 구현됨.
- 환불은 부모 트랜잭션에 `REQUESTED`를 저장한 뒤 커밋 후 실행하며, 미완료 상태를 같은 멱등키로 복구함.
- 알림은 도메인 트랜잭션과 함께 outbox를 저장하고 커밋 후 비동기로 발송하며, 채널 외부 호출은 실패 결과 집계와 제한 큐로 보호함.
- Spring Security 회원/관리자 체인과 SPA CSRF, Google/Naver의 필수 OAuth state 검증, 개인정보 암호화·블라인드 인덱스 전환은 구현됨.
- 장기 계약은 `docs/PRD/0001_기준_스펙/spec.md`, `docs/PRD/0004_API_계약/spec.md`와 관련 ADR을 기준으로 확인함.

### 다음 작업

1. `plan.md`의 장바구니 결제 우회 경로를 먼저 정리한다.
2. `docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md`에 맞는 k3s 운영 산출물을 구현한다.
3. `happygallery-identity-flows`와 `happygallery-notification-flows`를 함께 사용해 인증 코드 SMS와 회원가입 휴대폰 소유 확인을 순서대로 구현한다.

### 먼저 열 파일

- 결제 후속: `plan.md`, `application/src/main/java/com/personal/happygallery/application/payment/context/`, `application/src/main/java/com/personal/happygallery/application/cart/`, `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/MeCartController.java`, `frontend/src/features/cart/`
- 운영 구성: `docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md`, `happygallery-deploy-ops`
- 신원 경로: `application/src/main/java/com/personal/happygallery/application/customer/`, `application/src/main/java/com/personal/happygallery/application/booking/DefaultGuestBookingService.java`, `domain/src/main/java/com/personal/happygallery/domain/booking/PhoneVerification.java`, `happygallery-identity-flows`, `happygallery-notification-flows`

### 세션 전용 주의

- Flyway SQL 최고 번호는 V45이고, Java migration `V46__ProtectPlaintextPersonalData`가 추가로 존재한다. 다음 migration 번호를 정할 때 Java migration도 포함한다.
- 장바구니 checkout은 현재 결제 우회 경로다. 새 기본 계약으로 간주하지 않는다.
- 회원 signup은 현재 휴대폰을 `phoneVerified=false`로 저장한다. 전용 소유 확인이 끝나기 전에 true로 바꾸지 않는다.
