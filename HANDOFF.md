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

## 🚧 진행 중: 돈·신원 경로 복원 플랜 (Phase 1 백엔드 전환 구현됨)

**갱신 시점**: 2026-04-26
**브랜치**: `payment-integration`
**플랜 전문**: `~/.claude/plans/imperative-greeting-barto.md` — **반드시 먼저 읽는다.**

### 이번 세션에서만 결정된 사실 (다른 문서에 없음)
- **Flyway 번호 shift**: V31이 이미 `cleanup_redundant_indexes`로 점유되어, 플랜의 V31/V32 → **V32/V33** 으로 shift. 다음 세션도 이 규약 유지.
- **8회권 사용 예약**: amount=0 → PG 우회, `confirm` 직호출. 프론트가 이 분기를 처리.
- **빌드 검증 (2026-04-26)**: `:application:policyTest` PASS, `:bootstrap:bootJar -x test` BUILD SUCCESSFUL.

### 다음 세션 진입점 (남은 Task)

1. **테스트 마이그레이션 (테스트 에이전트 담당)** — `:application:test` 35건 실패. 제거된 엔드포인트(`POST /api/v1/me/bookings`, `/api/v1/me/orders`, `/api/v1/orders`, `/api/v1/bookings/guest`, `/api/v1/me/passes`)를 호출하는 6개 파일을 `/api/v1/payments/prepare` + `/confirm` 흐름으로 재작성:
   - `application/src/test/java/.../application/pass/PassCreditUsageWebUseCaseIT.java`
   - `application/src/test/java/.../application/pass/PassCreditUsageUseCaseIT.java`
   - `application/src/test/java/.../application/pass/PassCreditUsageFixture.java`
   - `adapter-in-web/src/test/java/.../adapter/in/web/customer/MePassUseCaseIT.java`
   - `adapter-in-web/src/test/java/.../adapter/in/web/customer/MeOrderUseCaseIT.java`
   - `adapter-in-web/src/test/java/.../adapter/in/web/customer/MeBookingUseCaseIT.java`
2. **신규 결제 통합 테스트 (테스트 에이전트 담당)** — `application/src/test/java/.../application/payment/`에 `PaymentPrepareUseCaseTest`, `PaymentConfirmUseCaseIT`(Fake confirm → 도메인 저장 + 금액 변조 거부) 작성.
3. **운영 수동 검증** — docker compose mysql/redis 기동 → `:bootstrap:bootRun` → 프론트에서 8회권 구매 / 주문 / 예약(예약금 + 8회권 사용) 결제 흐름 1회씩.

### Phase 진행도 / 환경 변수 / 플랜 밖 미룬 항목
→ `~/.claude/plans/imperative-greeting-barto.md` 참조. 여기엔 옮기지 않는다.

### 이 섹션 보존 규칙
- **Phase 3 완료까지 삭제 금지.** 표현 정리 명목으로도 덮지 않는다.
- 내용 변경은 플랜 파일에서 하고, 여기엔 "다음 세션 진입점"만 갱신한다.
