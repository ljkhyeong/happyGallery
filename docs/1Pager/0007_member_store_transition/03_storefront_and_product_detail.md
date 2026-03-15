# U3. Storefront And Product Detail

이 단위는 프론트 IA를 "기능 카드 모음"에서 "상점형 홈 + 상품 상세 구매" 구조로 바꾸는 작업이다.

현재 상태:
- U3 1차 완료
- 홈 / 네비게이션 / 상품 상세 / legacy `/orders/new` fallback 위치 재정의 반영
- 상품 상세의 guest CTA가 `/orders/new?productId=&qty=`로 이동하며 fallback 주문 항목 prefill 반영
- 남은 후속은 추가 머천다이징 정보와 guest fallback UX 고도화

성격:
- 프론트 중심
- 공개 조회 API 재사용 우선

---

## 1. 목표

- 홈과 네비게이션을 상점형으로 재구성한다.
- 상품 상세 페이지를 실제 구매 진입점으로 만든다.
- 현재 `/orders/new` 중심 주문 UX를 상품 상세 중심으로 내린다.

---

## 2. 범위

포함:
- 홈 화면 재구성
- 상단 네비게이션 재구성
- 상품 상세 구매 패널
- `/products/:id`에서 수량/합계/구매 CTA 제공
- 레거시 `/orders/new` 위치 재정의
- guest 주문 fallback prefill (`productId`, `qty`)

제외:
- 회원/비회원 최종 인증 제출 로직
- 예약/8회권 제출 직전 인증 게이트

---

## 3. 목표 UX

- 사용자는 홈에서 상품, 체험, 8회권을 상점처럼 탐색한다.
- 상품 상세에서 가격, 유형, 구매 가능 상태, 수량, 이행 방식, 구매 버튼이 한 화면에 보인다.
- 모바일에서 하단 고정 CTA 또는 유사한 구매 영역을 사용한다.
- 현재 "상품 상세는 정보만 보여주고 주문은 별도 페이지" 구조를 제거한다.

---

## 4. 주요 작업

- `frontend/src/pages/HomePage.tsx`
- `frontend/src/shared/ui/Layout.tsx`
- `frontend/src/pages/ProductDetailPage.tsx`
- `frontend/src/features/order/**`
- 상품 상세 구매용 `OrderComposer` 또는 동등한 feature 모듈 추가
- 필요 시 `OrderCreatePage`는 단일 상품 직접 진입 시 fallback 페이지로 축소
- 현재 `OrderCreatePage`는 상품 상세에서 넘어온 `productId`, `qty` query를 초기 주문 항목으로 반영한다

보조 개선:
- 관련 상품/추천 섹션은 API가 없으면 placeholder 없이 생략
- 이미지가 없으면 텍스트/배지/가격 중심 상세 레이아웃으로 먼저 간다

---

## 5. 권장 레이아웃

- 상단:
  - 브랜드
  - 상품
  - 체험 예약
  - 8회권
  - 내 정보 또는 로그인
- 상품 상세:
  - 상품명
  - 상태 배지
  - 가격
  - 유형 설명
  - 수량 선택
  - 총액
  - 구매 버튼
  - 배송/픽업 안내

---

## 6. 완료 기준

- `/products/:id` 에서 바로 주문 흐름 진입 가능
- 비회원도 상품 상세에서 fallback 주문 페이지로 이동할 때 상품/수량이 유지됨
- 홈과 네비게이션이 기능 카드 모음이 아니라 스토어 구조로 보임
- 모바일 기준에서 구매 CTA 접근성이 좋아짐

---

## 7. 최소 검증

- `cd frontend && npm run build`
- 모바일/데스크톱 대표 폭 수동 확인

---

## 8. 다음 단위로 넘길 산출물

- 상품 상세 구매 패널 UI
- 주문 생성 진입 시 필요한 context 값
- guest fallback prefill query 규칙 (`productId`, `qty`)
- 로그인/guest checkout 모달이 꽂힐 위치
