# HANDOFF

> **이 파일은 다음 AI 에이전트가 작업을 이어받기 위한 인수인계 전용이다. 사람용 문서가 아니다.**

## 작성 지침 (필수)

목적: 컨텍스트 부족 또는 에이전트 교체 때 다음 에이전트가 바로 이어서 작업하게 하는 것. 그 이상도 이하도 적지 않는다.

**적는다**
1. 지금 진행 중인 작업: 어디까지 했고, 다음 무엇을 해야 하는지, 어느 파일/플랜/스킬을 먼저 열어야 하는지.
2. 이 세션 안에서만 알 수 있고 다른 문서엔 없는 결정.
3. 보존 규칙이 명시된 진행 중 섹션: 규칙에 적힌 종료 조건까지 유지한다.

**적지 않는다 (다른 문서에 있으면 경로만 남긴다)**
- 사람용 설명, 프로젝트 소개, 완료 이력, 긴 배경, 일반 운영 규칙.
- 모듈 구조, 운영 주소, 인증 방식, 환경 구성, 배포 구조 → `README.md` / `CLAUDE.md`
- 우선 확인 문서 목록, 자주 쓰는 명령, 도구 사용 규칙 → `CLAUDE.md`
- 제품 요구사항 / API 계약 → `docs/PRD/`
- 설계 결정 배경 → `docs/ADR/`
- 전체 로드맵, 완료된 Phase 체크리스트 → `plan.md` 또는 `~/.claude/plans/*.md`
- "현재 활성 목표" 같은 추상적 우선순위. 진행 중 작업이 없으면 "진행 중 작업 없음" 수준으로 비운다.

**갱신 규칙**
- 작업이 끝났거나 보존 규칙이 만료되면 해당 섹션을 삭제한다. 기록 보관 목적이면 `docs/Retrospective/`로 옮긴다.
- 같은 세션 안에서 "표현 정리" 명목으로 진행 중 섹션을 덮지 않는다.
- 길이 목표: 진행 중 섹션 포함 80줄 이내. 넘으면 원문 문서나 플랜으로 옮기고, 여기는 다음 에이전트가 열어야 할 경로와 한 줄 이유만 남긴다.

---

## 🚧 진행 중: 돈·신원 경로 복원 플랜 (Phase 1 결제 경로 전환·테스트 보강됨)

**갱신 시점**: 2026-04-26
**브랜치**: `main` 기준으로 다음 작업 브랜치 생성
**플랜 전문**: `~/.claude/plans/imperative-greeting-barto.md` — **반드시 먼저 읽는다.**

### 이번 세션에서만 결정된 사실 (다른 문서에 없음)
- **Flyway 번호 shift**: V31이 이미 `cleanup_redundant_indexes`로 점유되어, 플랜의 V31/V32 → **V32/V33** 으로 shift. 다음 세션도 이 규약 유지.
- **8회권 사용 예약**: amount=0 → PG 우회, `confirm` 직호출. 프론트가 이 분기를 처리.
- **테스트 보강 (2026-04-26)**: 기존 예약/주문/8회권 생성 테스트를 `/api/v1/payments/prepare` + `/confirm` 경로로 전환. `PaymentPrepareUseCaseTest`, `PaymentConfirmUseCaseIT` 추가.
- **환불 참조값 연결 (2026-04-26)**: confirm 성공 시 `PaymentAttempt.pgRef`와 도메인 `payment_key`를 저장하고, 예약/주문 환불 생성 시 원결제 참조값을 `Refund.pgRef` 초기값으로 사용.
- **프론트 결제 흐름 보강 (2026-04-26)**: `ProductDetailPage` 회원 BUY NOW를 Toss prepare/confirm 경로로 전환. P8 E2E는 Toss stub 기반 현재 UI selector로 갱신.
- **E2E 실행 단위 (2026-04-26)**: `frontend npm run e2e`는 `@smoke` 4개만 실행. 전체는 `npm run e2e:full`, 도메인별은 README의 프론트엔드 명령 참조. 운영 기준은 `docs/ADR/0027_테스트_전략과_최소_테스트_세트_기준선/adr.md`, 회고는 `docs/Retrospective/0009_프론트_E2E_실행_시간_슬림화/retrospective.md`.
- **로컬 E2E 설정 (2026-04-26)**: 반복 smoke에서는 `RATE_LIMIT_ENABLED=false`로 bootRun. 관리자 예약 목록의 guest-only NPE는 `DefaultAdminBookingQueryService`에서 null userId 방어.
- **검증 (2026-04-26)**: E2E 슬림화 변경은 `frontend npx tsc -p tsconfig.node.json`, `frontend npm run build`, `frontend npm run e2e`(4 passed, 21.8s), `frontend npm run e2e:full`(9 passed, 34.6s), `git diff --check` PASS. 백엔드 bootRun은 중지했고 8080 리스너 없음.
- **API 계약 문서화 (2026-04-26)**: `adapter-in-web`에 Spring REST Docs 기반 `restDocsTest` 태스크와 공개/회원/관리자 API 계약 테스트를 추가. `./gradlew --no-daemon :adapter-in-web:restDocsTest`, `./gradlew --no-daemon :adapter-in-web:test` PASS.

### 다음 세션 진입점 (남은 Task)

1. **Phase 1 후속 잔여** — `plan.md`의 `P1R-T1b`, `P1R-T2`, `P1R-T4`, `P1R-T5`, `P1R-T6`, `P1R-T7`, `P1R-T8b` 확인.
2. **Phase 2 착수** — SMS 인증 실발송: `~/.claude/plans/imperative-greeting-barto.md`의 Phase 2와 notification 관련 스킬/문서 먼저 확인.

### Phase 진행도 / 환경 변수 / 플랜 밖 미룬 항목
→ `~/.claude/plans/imperative-greeting-barto.md` 참조. 여기엔 옮기지 않는다.

### 이 섹션 보존 규칙
- **Phase 3 완료까지 삭제 금지.** 표현 정리 명목으로도 덮지 않는다.
- 내용 변경은 플랜 파일에서 하고, 여기엔 "다음 세션 진입점"만 갱신한다.
