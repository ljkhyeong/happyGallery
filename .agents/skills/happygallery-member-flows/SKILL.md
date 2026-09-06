---
name: happygallery-member-flows
description: happyGallery의 /api/v1/me 회원 API·인증 필터·마이페이지 데이터·비회원 기록 연결을 변경할 때 사용한다. 가입·로그인·OAuth·휴대폰 소유 검증은 identity 스킬을 사용한다.
---

# happyGallery 회원 API·기록 연결

## 규칙

- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/`와 `security/customer/`, `application/src/main/java/com/personal/happygallery/application/customer/`를 확인한다.
- `CustomerAuthenticationFilter`가 회원 세션을 검증하고 익명 인증 전에 `CustomerPrincipal`을 구성한다. `/me/**`는 `ROLE_CUSTOMER`를 요구하고 controller는 `@AuthenticationPrincipal`의 `userId()`를 유스케이스에 전달한다.
- 보안 설정·필터·OAuth resolver·처리율 제한의 경로는 `CustomerSecurityRoutes`를 함께 사용한다.
- 비회원 기록 연결은 휴대폰 소유 인증 후 실행한다. `PhoneVerificationRequiredException`은 클라이언트의 인증 단계로 연결한다.
- 전화번호는 `KoreanPhoneNumber`로 한 번 정규화하고 HMAC 인덱스로 조회한다. `userId == null`인 비회원 기록만 연결한다.
- 이용권 회원 예약은 소유권 확인·슬롯 잠금·예약 생성·횟수 차감을 같은 트랜잭션에서 처리한다. 유료 예약·주문·이용권 구매는 공통 결제 prepare/confirm을 사용한다.
- 회원 데이터·비회원 복구 정보가 로그인 계정 변경 후 남지 않도록 화면 수정에는 `happygallery-frontend-flows`의 세션 규칙을 적용한다.

## 검증

- 인증 필터는 `CustomerAuthUseCaseIT`·`SecurityBoundaryUseCaseIT` 중 관련 시나리오, 기록 연결은 `CustomerGuestClaimUseCaseIT`를 실행한다.
- 회원 예약·주문·이용권은 해당 `Me*UseCaseIT`를 선택하고 서비스도 바뀔 때 해당 application 테스트를 추가한다. HTTP 변경은 `api-contract`를 함께 적용한다.
