---
name: happygallery-identity-flows
description: happyGallery의 가입·로그인·Google/Naver OAuth·재인증·휴대폰 소유 확인·탈퇴를 변경할 때 사용한다. 인증 후 회원 데이터 API는 member 스킬을 사용한다.
---

# happyGallery 회원 인증·휴대폰 확인

## 인증 규칙

- `application/src/main/java/com/personal/happygallery/application/customer/`, 웹 `security/customer/`, 외부 adapter의 `oauth/`를 확인한다. 관련 설계는 ADR-0023·0024·0035·0036·0040에 있다.
- 인증·OAuth·결제·처리율 제한 경로는 `CustomerSecurityRoutes`를 원본으로 사용한다.
- 비밀번호 로그인 facade는 `Propagation.NEVER`를 유지한다. login snapshot 조회와 첫 BCrypt 비교는 트랜잭션 밖에서 수행한다. 성공 후보만 별도 트랜잭션에서 회원 행을 잠가 활성 상태·로컬 비밀번호·변경된 hash를 재검사하고 저장한다.
- BCrypt에 전달하는 모든 웹 비밀번호는 UTF-8 72바이트 이하로 제한한다. 서버 Bean Validation과 공용 프론트엔드 byte helper를 사용한다.
- 로그인·가입 성공은 현재 세션에 10분 재인증 증명을 둘 수 있다. 계정 연결·해제, 전화번호 등록·변경, 탈퇴는 본인 소유 로그인 수단으로 재인증하고 `userId`·`credentialVersion`에 증명을 묶는다.
- 재인증이 필요한 command는 예상 credential version을 전달하고 회원 행 잠금 후 비교한다. 탈퇴 확인 문구 입력만으로 본인 인증을 대체하지 않는다.
- 탈퇴를 막는 활동 생성과 탈퇴는 모두 `MemberAccountGuard`로 회원 행을 먼저 잠근다. 진행 중 결제·주문·클레임·예약·취소 후속 작업·이용권·환불을 같은 기준으로 확인한다.
- 일반 `UserReaderPort`는 활성 회원만 읽고, 과거 관리자 조회만 익명화된 탈퇴 회원을 포함하는 history 조회를 사용한다.

## 휴대폰·가입 규칙

- 가입은 같은 정규화 전화번호의 활성·미사용 `SIGNUP` 코드를 요구한다. 코드 소비와 회원 생성을 같은 트랜잭션에서 처리하고 성공한 회원만 `phoneVerified=true`로 저장한다.
- 코드의 목적은 저장 행·HMAC·발송 완료·소비 쿼리에 모두 포함한다. 다른 목적으로 발급한 코드는 수락하지 않는다. `VerifiedGuestResolver`의 비회원 기록 upsert를 회원 가입에 재사용하지 않는다.
- `PhoneVerificationAttemptGuard`는 회원·상품·재고·슬롯 DB 작업 전에 facade에서 실행한다. 코드 행 잠금·1회 소비·보호할 업무 쓰기는 하나의 짧은 트랜잭션에서 처리한다.
- 인증 SMS는 전용 `PhoneVerificationSender`로 트랜잭션 밖에서 발송한다. `delivered`는 NHN 접수 성공이며 실제 수신을 뜻하지 않는다. 접수 성공을 기록한 코드만 사용 가능하다.
- 발송 완료는 행 잠금 아래 같은 전화번호·목적의 더 낮은 ID만 무효화한다. 뒤늦게 완료한 과거 코드를 재활성화하지 않는다. local/E2E 코드 조회도 전화번호·목적을 함께 사용한다.
- 인증 코드 조회 도우미는 local/dev에서만 제공한다. 운영 응답·로그에 코드를 남기지 않는다.
- 가입 동의는 현재 약관·개인정보 버전을 서버에서 검증·저장한다. 설정 버전은 비어 있지 않고 컬럼 길이 이하여야 하며, 동의 시각은 서버가 정한다.
- OAuth·소셜 가입·계정 연결을 바꾸면 [소셜 인증 규칙](references/social-auth.md)을 읽는다. SMS 전송 자체는 `happygallery-notification-flows`를 함께 사용한다.

## 검증

- 변경에 맞게 `CustomerAuthUseCaseIT`, `CustomerCredentialUseCaseIT`, `MemberPhoneRegistrationUseCaseIT`, `PhoneOwnershipVerificationUseCaseIT`, `RateLimitFilterTest`를 선택한다.
- HTTP 변경은 `api-contract`, 화면 인증 흐름 변경은 `happygallery-frontend-flows`를 함께 적용한다.
