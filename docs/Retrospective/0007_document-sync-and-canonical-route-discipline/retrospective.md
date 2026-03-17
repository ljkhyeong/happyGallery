# 문서 동기화와 canonical route 운영 규율 회고

**날짜**: 2026-03-17  
**상태**: Retrospective note

---

## 배경

`main` 히스토리에는 문서 정합화와 경로 정리 커밋이 반복된다.

대표 커밋:
- `1316409` spec 작성
- `4741060` 범위 외 문서 분리
- `8eb72f5` 문서 현행화
- `643f91d` 프론트 MVP와 공개 API, 문서 최신화 반영
- `3ca53dd` 회원 스토어 셀프서비스와 guest claim 흐름 정리
- `364ee1f` 관측성 스택과 운영 문서 정리 반영

라우트도 `/orders/new`, `/guest`, `/guest/orders`, `/guest/bookings`, `/my/**`가 자리잡기 전까지
여러 번 설명/문서/E2E를 같이 바꾸는 비용이 있었다.

---

## 관찰

- 기능 구현 뒤에 README/HANDOFF/PRD/E2E 동기화가 follow-up으로 반복된다.
- alias 경로와 canonical 경로가 한동안 같이 존재하면 운영 카피와 테스트 selector도 흔들린다.
- 문서 종류별 역할이 늦게 분리될수록 core spec이 무거워진다.

---

## 회고 포인트

### 1. 새 기능 PR에 문서 체크리스트를 붙인다

As-Is:
- 문서 반영 여부가 PR마다 암묵적으로 처리된다.

To-Be:
- PR 템플릿이나 작업 규칙에 “README / PRD / API contract / E2E / 운영 문서” 체크 항목을 둔다.

### 2. 공개 경로를 만들 때 canonical route를 먼저 선언한다

As-Is:
- 경로 alias와 운영 문구가 뒤늦게 정리된다.

To-Be:
- 새 공개 경로를 도입할 때 canonical route, alias, 제거 시점, 운영 카피 기준을 같이 적는다.

### 3. 문서 분리 기준을 더 일찍 적용한다

As-Is:
- 기능 설명, 설계 메모, 테스트 가이드가 한 문서에 모이기 쉽다.

To-Be:
- 요구사항은 PRD, 실험은 POC, backlog는 Idea, 작은 개선은 `simple-idea.md`로 일찍 분리한다.

---

## 현재 결론

히스토리상 문서와 경로 정리는 “마지막에 한 번 더 하는 일”이 되기 쉬웠다.  
다음부터는 기능 설계 단계에서 canonical route와 문서 분리 위치를 먼저 정하는 편이 정리 비용이 낮다.
