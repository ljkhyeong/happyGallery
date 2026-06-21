# Trigger Audit

Goal: keep each happyGallery skill narrow enough that a typical request has an obvious primary skill.

## Prompt routing samples

1. "예약 변경 가능 시간 경계 로직 고쳐줘" -> `happygallery-booking-flows`
2. "게스트 예약 access token 조회 API 응답 수정해줘" -> `happygallery-booking-flows`
3. "8회권 만료 배치에서 남은 크레딧 소멸 규칙 점검해줘" -> `happygallery-pass-flows`
4. "pass refund 계산식을 바꾸고 미래 예약 취소도 같이 반영해줘" -> `happygallery-pass-flows`
5. "주문 승인 후 24시간 자동환불 배치가 이상해" -> `happygallery-order-flows`
6. "제작 주문 complete-production 처리 바꿔줘" -> `happygallery-order-flows`
7. "PG timeout일 때 refund 실패를 어떻게 기록하는지 고쳐줘" -> `happygallery-payment-flows`
8. "admin/refunds/failed 응답 필드 바꿔줘" -> `happygallery-admin-flows`
9. "관리자 페이지에서 401 처리와 토스트 UX를 손봐줘" -> `happygallery-frontend-flows`
10. "상품 등록 API에서 inventory 생성도 같이 묶어줘" -> `happygallery-product-flows`
11. "D-1 예약 리마인드 배치에서 중복 알림이 나가" -> `happygallery-notification-flows`
12. "BatchScheduler cron 시간 바꿔줘" -> `happygallery-batch-flows`
13. "policy/useCaseTest 선택 기준이랑 docs sync 규칙부터 보고 전체 리팩토링 진행해줘" -> `happygallery-spring-backend`

## Tuning notes

- `booking`에서 `pass` 관련 키워드를 제거해 `pass` 전용 skill이 먼저 트리거되게 했다.
- `order`에서 `payment refund` 표현을 `refund execution`으로 좁혀 payment skill과의 직접 충돌을 줄였다.
- `spring-backend`는 fallback 역할만 하도록 "several modules", "broad repository review", "shared Gradle or module structure" 같은 범용 표현 위주로 유지했다.
- `admin`은 운영자 backend endpoint, filter, auth, and `X-Admin-Id` 같은 표현에 집중시키고, frontend admin page work는 `happygallery-frontend-flows`로 분리했다.
