---
name: happygallery-code-review
description: happyGallery의 미커밋 변경·브랜치 diff·commit·PR을 결함 중심으로 리뷰할 때 사용한다. 보안 전용 점검은 happygallery-security-review, 받은 리뷰 코멘트 대응은 happygallery-github-flows를 사용한다.
---

# happyGallery 코드 리뷰

## 대상과 원칙

- 대상을 먼저 확정한다. 미커밋은 `git diff HEAD`와 새 파일, 브랜치는 `git merge-base HEAD <기준>`부터의 diff, PR은 `gh pr diff <PR>`을 사용한다. 기준은 `main`이며 원격이 앞서 있으면 `origin/main`을 쓴다.
- 읽기 전용으로 리뷰한다. 파일 수정·commit·push·PR 코멘트 작성은 사용자가 요청한 경우에만 한다.
- 변경 파일마다 담당 도메인 스킬을 읽고 그 규칙을 기준으로 삼는다. 도메인 규칙을 이 스킬에 복제하지 않는다.
- 이번 변경이 만든 문제이고, 호출 경로나 입력으로 실패 시나리오를 보일 수 있으며, 작성자가 알면 고칠 문제만 보고한다. 기존 문제·의도한 동작 변경·취향 차이는 제외한다.

## 리뷰 관점

- 의존 방향: `bootstrap → adapter → application → domain` 위반, adapter에 들어간 업무 규칙, 계층에 맞지 않는 검증 위치.
- 트랜잭션: PG·SMS·스마트스토어 호출이 트랜잭션 안에 있는지, 잠금 순서와 잠금 후 재조회, `AFTER_COMMIT` 실행 거절 처리.
- 동시성·멱등성: 정원·재고·횟수 차감의 잠금, 재시도 시 중복 실행, 저장한 idempotency key 재사용.
- 시간·상태: 업무 코드의 직접 `now()` 호출, 서울 시각 해석, 도메인 전이 메서드를 우회한 상태 변경.
- 계약·schema: Controller·웹 DTO 변경 시 REST Docs·OpenAPI·생성 client 동시 갱신, 생성 파일 직접 편집, 적용된 migration 수정, 엔티티와 schema 불일치.
- 개인정보·인가: 소유권 검사 누락, 평문 PII·token의 로그·URL·응답 노출, 공개 SSR loader에 들어간 개인 데이터.
- 테스트·문서: 변경 위험을 검증하는 테스트 유무와 `AGENTS.md`가 금지한 구현 복제 테스트, 동작 변경에 따른 PRD·ADR·README 누락.
- 중복: 같은 검증·변환을 다른 계층이 이미 수행하는지 `rg`로 확인한다.

## 결과 형식

- 심각도 순으로 `[P1] 고칠 내용 — 경로:라인`을 적고, 다음 문단에 실패 시나리오와 이유를 쓴다. 인용 범위는 diff와 겹치게 최소로 잡는다.
- P0은 배포 차단·데이터 손상, P1은 바로 고칠 결함, P2는 일반 결함, P3는 영향은 작지만 고칠 가치가 있는 문제다.
- 결함이 없으면 `발견 사항 없음.`이라고 쓴다. 마지막에 전체 평가와 검증하지 못한 위험·테스트 공백을 짧게 적는다.
