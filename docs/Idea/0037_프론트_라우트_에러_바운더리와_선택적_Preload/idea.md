# 프론트 라우트 Error Boundary 와 선택적 Preload 검토

> React lazy 라우트에서 청크 로드 실패나 일시적 네트워크 장애가 발생할 때의 사용자 안내를 개선하고, 자주 이동하는 경로는 hover/focus 시점에 선로딩해 체감 전환 속도를 높이는 방안을 검토한다.

---

## 배경

현재 프론트는 [App.tsx](../../../../frontend/src/app/App.tsx) 에서 `React.lazy` + `Suspense` 기반으로 다수의 페이지를 분리 로드한다. 전역 `ErrorBoundary`가 있어 예외 자체는 잡을 수 있지만, lazy import 실패도 동일한 전역 오류 화면으로 떨어진다.

이 구조는 최소한의 장애 방어는 되지만 다음 한계가 있다.

- 특정 페이지 청크만 실패해도 앱 전체가 전역 오류 화면으로 보일 수 있다.
- lazy route 전환 직전까지는 관련 청크를 내려받지 않으므로 첫 진입 지연이 생긴다.
- 상단 메뉴나 홈 CTA처럼 이동 가능성이 높은 경로도 현재는 preload 힌트가 없다.

---

## 문제 정의

### 1. 라우트 단위 장애 안내 부족

- `Suspense`는 로딩 상태만 처리하고, 청크 fetch 실패는 처리하지 못한다.
- 현재 전역 `ErrorBoundary`는 fallback 메시지가 넓고 일반적이라 “이 페이지만 다시 시도하면 되는 상황”과 잘 맞지 않는다.

### 2. 첫 진입 체감 지연

- `/login`, `/signup`, `/my`, `/cart`, `/passes/purchase` 같은 경로는 실제 전환 빈도가 높다.
- 이런 경로는 클릭 직후 lazy import가 시작되어 전환 텀이 눈에 띌 수 있다.

---

## 제안

### A. LazyRoute 주변에 route-scoped Error Boundary 추가

- 전역 `ErrorBoundary`는 유지한다.
- 각 lazy route 래퍼(`LazyRoute`) 또는 공통 route wrapper 안에 페이지용 error boundary 를 추가한다.
- fallback 문구는 전역 장애와 구분한다.

예시 메시지:

- `페이지를 불러오지 못했습니다.`
- `네트워크 상태를 확인한 뒤 다시 시도해 주세요.`
- `다시 시도` 버튼 또는 `새로고침` 버튼 제공

기대 효과:

- lazy import 실패를 전역 장애처럼 보이지 않게 할 수 있다.
- 한 페이지 실패와 앱 전체 장애를 구분해 보여 줄 수 있다.

### B. 선택적 preload 적용

- 모든 lazy route 에 preload 를 거는 대신, 실제 전환률이 높은 경로에만 적용한다.
- 후보:
  - `/login`
  - `/signup`
  - `/my`
  - `/cart`
  - `/passes/purchase`
- 트리거:
  - `onMouseEnter`
  - `onFocus`
  - 필요 시 홈 진입 후 idle 시점 preload

기대 효과:

- 자주 사용하는 진입 경로에서 클릭 후 대기 시간을 줄일 수 있다.
- 전면 preload 보다 네트워크 비용을 통제하기 쉽다.

---

## 적용 원칙

### 지금 바로 하지 않을 것

- 모든 라우트에 일괄 preload
- 전역 `ErrorBoundary` 제거
- preload 때문에 번들 다운로드가 급격히 늘어나는 구성

### 적용 시 지킬 것

- 전역 boundary 는 예상치 못한 전체 장애용으로 유지
- route-scoped boundary 는 lazy 청크 실패나 페이지 렌더링 실패 대응에 집중
- preload 대상은 실제 전환률이 높은 링크만 선택
- 모바일/저속 네트워크에서 과도한 선다운로드가 발생하지 않도록 범위를 좁힘

---

## 구현 스케치

### Route boundary

- `LazyRoute` 내부를 `ErrorBoundary -> Suspense -> page` 순서로 감싼다.
- 필요하면 전역 `ErrorBoundary`와 다른 전용 fallback UI 컴포넌트를 둔다.

### Route preload

- 각 lazy import 를 다음처럼 분리한다.

```ts
const loadLoginPage = () => import("@/pages/LoginPage");
const LoginPage = lazy(() => loadLoginPage().then((module) => ({ default: module.LoginPage })));
```

- 링크에서 preload 함수를 직접 호출한다.

```ts
onMouseEnter={loadLoginPage}
onFocus={loadLoginPage}
```

---

## 판단

- route-scoped Error Boundary 는 적용 가치가 높다.
- preload 는 선택적 적용이면 가치가 있지만, 전면 도입은 과할 수 있다.
- 따라서 우선순위는 `route-scoped Error Boundary -> 상위 몇 개 경로의 선택적 preload` 순서가 적절하다.
