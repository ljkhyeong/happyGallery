---
name: happygallery-frontend-flows
description: happyGallery의 React Router 화면·SSR·스타일·폼·React Query·생성 API 사용을 변경할 때 사용한다. 서버 계약·인증·도메인도 바뀌면 해당 백엔드 스킬을 함께 사용한다.
---

# happyGallery 화면

## 구조와 규칙

- 라우트는 `frontend/src/routes.ts`, 페이지는 `frontend/src/pages`, 기능은 `frontend/src/features`, 공용 API·UI는 `frontend/src/shared`를 확인한다. 실제 의존성과 script는 `frontend/package.json`을 기준으로 한다.
- 공개 SSR loader는 공용 데이터만 처리한다. 회원·비회원 개인 데이터와 인증 결과를 공용 캐시에 넣지 않는다. 잘못된 공개 URL은 실제 HTTP 404를 반환한다.
- React Query와 공용 `api()`·`ApiError`를 사용한다. Orval 함수도 `generatedApiClient`를 거쳐 cookie·CSRF·timeout·Sentry 처리를 유지한다.
- 생성 React Query hook을 추가하지 않는다. query key·캐시 정책·무효화는 feature가 관리하고 서버 DTO는 생성 타입을 사용한다. 명세가 잘못되면 `api-contract`로 서버 원본을 고친다.
- 인증·결제·회원 데이터·비회원 복구 상태를 변경하면 [세션과 비동기 결과](references/session-state.md)를 읽는다.
- `ErrorAlert`, `LoadingSpinner`, `EmptyState`, `StatusBadge`, 공용 toast를 재사용한다. 비동기 환불의 REQUESTED 응답을 환불 완료로 표시하지 않는다.
- offset 없는 서버 날짜·시각은 서울 시각으로 해석한다. 브라우저 시간대로 예약·픽업·관리자 폼 시간을 바꾸지 않는다.
- Pretendard·Bootstrap 기반 스타일을 사용한다. `global.scss`는 import 순서만 관리하고 규칙은 담당 partial에 둔다.
- Toss 화면은 `VITE_TOSS_CLIENT_KEY`를 사용한다. 결제 prepare/confirm은 결제 스킬, 휴대폰 인증은 identity 스킬과 맞춘다. 운영 화면에 인증 코드를 노출하지 않는다.

## 검증

- 문구만 바꾸면 diff·사용처를 확인하고 JSX 변경은 `frontend`에서 `npm run typecheck`로 확인한다. 문구를 선택자로 쓰는 기존 테스트만 실행하며, 결제·인증 화면이라는 이유로 도메인 E2E 전체를 실행하지 않는다.
- component·query 로직은 typecheck와 해당 단위 테스트, 라우트·SSR·스타일·번들 설정은 `npm run build`를 선택한다. build가 typegen·tsc를 포함하므로 같은 입력의 typecheck를 연달아 실행하지 않는다.
- 화면 배치·반응형 변경은 관련 화면을 모바일·데스크톱에서 확인한다. 생성 API 변경은 `api-contract`의 생성·검토 절차를 따르고, 명세가 그대로면 재생성하지 않는다.
- 사용자 흐름이 바뀌면 관련 spec·시나리오를 먼저 선택한다. 여러 시나리오에 영향이 있으면 `e2e:payment`, `e2e:identity`, `e2e:admin`, 공용 흐름이 넓게 바뀌면 `e2e:full`로 확대한다.
- E2E를 실행하기 전 해당 시나리오의 backend·인증·fixture 준비 조건을 확인한다. 준비 단계에서 실패하면 원인을 해결한 뒤 해당 시나리오만 다시 실행하고, 미해결이면 미검증 항목으로 남긴다.
