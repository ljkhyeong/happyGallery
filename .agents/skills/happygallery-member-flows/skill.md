---
name: happygallery-member-flows
description: >
  Repository-specific workflow for the member-only `/api/v1/me/**` API surface in the happyGallery app
  (both backend and frontend). Use this skill whenever the request involves: CustomerAuthFilter,
  Me*Controller (MeBookingController, MeOrderController, MePassController, MeCartController,
  MeGuestClaimController, MeInquiryController, MeNotificationController, MeProductQnaController),
  guest-to-member claim mechanics (GuestClaimUseCase, MeGuestClaimController), member booking creation
  (MemberBookingUseCase / DefaultMemberBookingService), member order/pass purchase via the member API,
  MyPage / MyBookingDetailPage, or the customer-claim / my-booking frontend features. Also use this
  skill when someone says "게스트 예약을 회원으로 옮기고 싶어요" / "마이페이지에서 이용권을 못 찾겠어요" /
  "회원 인증 필터가 어떻게 동작해요?". Always use this skill when the request touches `/api/v1/me/**`
  or the member authentication boundary — do not split across booking, order, or pass skills when the
  entry point is the member /me API. For signup, login, Google OAuth, or phone ownership verification
  itself, use `happygallery-identity-flows` instead.
---

# happyGallery Member Flows

## Core references

- `docs/PRD/0001_기준_스펙/spec.md` — 회원 API 계약, 인증 흐름, 게스트 클레임 정책
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/CustomerAuthFilter.java` — 회원 인증 필터
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/` — Me*Controller 군 + GuestClaim 컨트롤러
- `application/src/main/java/com/personal/happygallery/application/customer/` — 회원/클레임 use case + port

## Scope vs `happygallery-identity-flows`

- 이 스킬: `/api/v1/me/**` 진입점, 인증 필터 뒤의 회원 전용 API 계약, 게스트→회원 claim 메커니즘 (이미 인증된 신원 기준).
- `happygallery-identity-flows`: 회원가입·로그인·Google OAuth·휴대폰 소유 검증 자체 (신원 형성 단계).
- SMS 발송 기반 변경은 `happygallery-notification-flows`. 단, 휴대폰 소유 규칙은 identity-flows 쪽이다.

## Architecture overview

```
CustomerAuthFilter → Me*Controller (/api/v1/me)
                       ├── /me/orders        (MeOrderController)
                       ├── /me/bookings      (MeBookingController, MemberBookingUseCase)
                       ├── /me/passes        (MePassController)
                       ├── /me/cart          (MeCartController)
                       ├── /me/guest-claims  (MeGuestClaimController, GuestClaimUseCase)
                       ├── /me/inquiries     (MeInquiryController)
                       ├── /me/notifications (MeNotificationController)
                       └── /me/product-qnas  (MeProductQnaController)
```

## Cross-cutting rules

`happygallery-spring-backend`의 Respect module boundaries·Repository constraints·Test writing rules, `api-contract`의 Non-negotiable invariants, `happygallery-test-refactor`의 Assertion conventions를 이 스킬에서도 항상 따른다.

## Non-negotiable invariants

- `CustomerAuthFilter`가 회원 세션을 검증하고 request attribute로 userId를 설정한다 — 모든 /me API는 이 attribute를 통해 userId를 획득한다 (`@CustomerUserId` resolver 사용).
- 게스트 클레임은 전화번호 인증이 완료된 경우에만 실행 가능. `PhoneVerificationRequiredException` 발생 시 클라이언트는 인증 단계로 이동해야 한다.
- 게스트 클레임 시 중복 소유 방지: `userId == null` 인 게스트 레코드만 claim 대상이다.
- 회원 예약 생성 시 pass를 사용하면 `MemberBookingUseCase` 구현(`DefaultMemberBookingService`)이 pass credit 차감을 처리한다 — 예약 생성과 credit 차감은 같은 트랜잭션 내에 있어야 한다.
- 회원 API 경로 `/api/v1/me/**` 는 반드시 `CustomerAuthFilter` 뒤에 위치해야 한다 — 공개 경로에 노출되지 않도록 한다.

## Guest Claim flow

```
GET  /api/v1/me/guest-claims/preview   → ClaimPreview (phoneVerified + 클레임 가능 목록)
POST /api/v1/me/guest-claims/verify    → 전화번호 인증 코드 확인 → ClaimPreview
POST /api/v1/me/guest-claims           → ClaimResult (실제 클레임 실행)
```

**전화번호 정규화**: 하이픈 유무 등 형태 차이를 수용하기 위해 GuestClaim 구현은 원본 + 숫자만 형태 둘 다 조회한다.

## Member booking via /me

`MemberBookingUseCase` (port) → `DefaultMemberBookingService` (application 모듈 구현).

- `passId`가 있으면 pass 예약, 없으면 일반 예약.
- 슬롯 잠금 (`SELECT ... FOR UPDATE`)은 기존 booking 흐름과 동일.
- 결제 진입은 `/api/v1/payments/prepare` + `/confirm` (자세한 건 `happygallery-payment-flows`).

## Likely code locations

**Backend:**
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/CustomerAuthFilter.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/Me*Controller.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/resolver/CustomerUserId.java`
- `application/src/main/java/com/personal/happygallery/application/customer/` — CustomerAuth/GuestClaim use case + port
- `application/src/main/java/com/personal/happygallery/application/booking/DefaultMemberBookingService.java`
- `application/src/main/java/com/personal/happygallery/application/booking/port/in/MemberBookingUseCase.java`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/GuestRepository.java`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/PhoneVerificationRepository.java`

**Frontend:**
- `frontend/src/pages/MyPage.tsx` — 마이페이지 진입점
- `frontend/src/pages/MyBookingDetailPage.tsx` — 예약 상세
- `frontend/src/features/customer-claim/` — 게스트 클레임 모달 + API
- `frontend/src/features/my-booking/` — 회원 예약 목록/상세

## High-value tests (for reference)

- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/customer/CustomerAuthUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/customer/CustomerGuestClaimUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/customer/Me{Booking,Order,Pass}UseCaseIT.java`

## Verification workflow

- 인증 필터 변경: `./gradlew :adapter-in-web:test --tests "*CustomerAuthFilter*" --tests "*RateLimit*"`
- 게스트 클레임 흐름 변경: `./gradlew --no-daemon :application:useCaseTest --tests "*GuestClaim*"`
- 회원 예약/주문/패스 API 변경: `./gradlew :adapter-in-web:test --tests "*Me*UseCaseIT*"` + `./gradlew --no-daemon :application:useCaseTest --tests "*Member*"`
- 프론트엔드 변경: `cd frontend && npm run build`

## Doc sync checklist

- 회원 API 경로/DTO/에러코드 변경: `docs/PRD/0001_기준_스펙/spec.md`, `docs/PRD/0004_API_계약/spec.md`
- 게스트 클레임 정책 변경: `docs/PRD/0001_기준_스펙/spec.md` 및 관련 ADR
- 결제 진입 변경 시: `happygallery-payment-flows`와 `docs/PRD/0004_API_계약/spec.md`의 `2.15 결제 API` 항목
- 현재 작업 상태: `HANDOFF.md`
