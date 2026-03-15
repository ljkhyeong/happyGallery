# CLAUDE.md

## Architecture
멀티모듈 Gradle (Spring Boot 4.0.2 / Java 21). 모듈 구조·레이어링 → `happygallery-spring-backend` 스킬 참조.

**핵심 도메인 열거형** (`domain` 모듈):
- `order`: OrderStatus, FulfillmentType, RefundStatus
- `booking`: BookingStatus, BalanceStatus, BookingHistoryAction
- `pass`: PassLedgerType
- `product`: ProductType, ProductStatus

**관측성**: `RequestIdFilter` → `X-Request-Id` MDC 주입 + 응답 반환. 로그 패턴: `%5p [%X{requestId:-}]`

**DB 마이그레이션**: Flyway 자동 적용 (`classpath:db/migration`). `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 환경변수로 오버라이드.

---

## 세션 시작 시
`HANDOFF.md`가 존재하면 가장 먼저 읽는다.

## 작업 시작 시 스킬 참조 규칙
1. `.claude/skills/`에서 도메인 매칭 스킬을 읽는다.
2. **non-negotiable invariants** → **verification workflow** → **doc sync checklist** 순서를 따른다.
3. 완료 보고 시 `사용: 스킬명` 형식으로 명시한다.

---

## Tool Usage Rules
| 작업 | 도구 |
|------|------|
| 파일 검색 | Glob (`find`/`ls` 금지) |
| 파일 읽기 | Read (`cat`/`head` 금지) |
| 내용 검색 | Grep (`grep`/`rg` 금지) |
| 파일 수정 | Edit (`sed`/`awk` 금지) |
| 그 외 시스템 명령 | Bash |

---

## Approval Minimization
- 승인 요청은 원격 쓰기·PR merge·워크스페이스 밖 쓰기 등 실제 escalation이 필요한 경우에만.
- 아래 명령은 처음부터 unsandboxed 실행: Gradle JVM (`test`, `:app:bootRun` 등), `gh pr *`, `git fetch/push/pull`, `docker exec`, Playwright
- `바로`/`지금`/`곧바로` 지시 시 비파괴적이면 재확인 없이 진행.
- PR은 mergeable 확인 후 충돌 없으면 바로 머지.
- 변경 범위를 벗어난 재검증·중복 조회 금지.

---

## 역할 분담
- 이 에이전트는 **구현(코드 작성)에만 집중**.
- 테스트·문서 동기화는 별도 에이전트 담당 — 직접 수행하지 않는다.
