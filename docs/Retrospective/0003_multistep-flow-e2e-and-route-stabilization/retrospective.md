# multi-step 흐름 E2E와 canonical route 안정화 회고

**날짜**: 2026-03-17  
**상태**: Retrospective note

---

## 가설

guest/member 전환처럼 단계가 긴 사용자 흐름은
브라우저 smoke와 canonical route 기준이 정해진 뒤에야 안정적으로 유지된다.

---

## 검증 방법

member store/self-service 확장 구간의 대표 커밋을 확인했다.

- `643f91d` 프론트 MVP와 공개 API, 문서 최신화 반영
- `3ca53dd` 회원 스토어 셀프서비스와 guest claim 흐름 정리
- `ca4f18e`, `6c4321e`, `835589c` member/guest 흐름 확장 merge 구간
- 이후 guest hub, canonical route, onboarding, claim, smoke 안정화 작업들

---

## 결과

- `/guest`, `/guest/orders`, `/guest/bookings`, `/orders/new`, `/my/**`가 자리잡기 전까지
  문서, 운영 카피, E2E selector, 사용자 안내가 자주 흔들렸다.
- Playwright smoke가 붙은 뒤에는 guest claim, member onboarding, direct fallback 같은 흐름의 회귀를 더 빨리 잡을 수 있었다.
- 즉, 복합 사용자 흐름은 API/컴포넌트 단위 검증만으로는 충분하지 않았고,
  route 기준과 브라우저 기준 검증이 같이 있어야 안정화가 빨랐다.

---

## 결론

multi-step 사용자 흐름에는 다음 두 가지를 초기에 같이 잡는 것이 유효했다.

1. canonical route와 fallback 정책 선언
2. 최소 브라우저 smoke 시나리오 확보

이 둘을 뒤로 미루면 프론트, 문서, 운영 카피를 반복 수정하게 된다.
