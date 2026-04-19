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

## 문서 구조 (`docs/`)
| 경로 | 설명 |
|------|------|
| `docs/PRD/` | 제품 요구사항과 운영 정책의 기준 문서. 기능 계약이나 정책 변경 시 먼저 맞춘다. |
| `docs/ADR/` | 데이터 모델, 상태 전이, 인증, 결제, 관측성, 헥사고날 전환 같은 핵심 설계 결정을 남긴다. |
| `docs/Idea/` | 정식 요구사항으로 확정하지 않은 아이디어, 향후 검토 메모, PRD 밖에서 관리하는 엔지니어링 가이드를 보관한다. |
| `docs/POC/` | 실제 실험 결과와 적용 판단 근거를 남긴다. |
| `docs/Retrospective/` | 지나온 변경 흐름을 되짚어 얻은 교훈과 회고를 남긴다. |
| `docs/1Pager/` | 이해관계자 공유용 요약 문서 카테고리다. |

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
- 아래 명령은 처음부터 unsandboxed 실행: Gradle JVM (`test`, `:bootstrap:bootRun` 등), `gh pr *`, `git fetch/push/pull`, `docker exec`, Playwright
- `바로`/`지금`/`곧바로` 지시 시 비파괴적이면 재확인 없이 진행.
- PR은 mergeable 확인 후 충돌 없으면 바로 머지.
- 변경 범위를 벗어난 재검증·중복 조회 금지.

---

## 문서 동기화
- 소스 수정 시 관련 문서(`docs/PRD/`, `docs/ADR/`, `simple-idea.md` 등)도 함께 갱신한다.
- 스킬의 **doc sync checklist**를 기준으로 갱신 대상을 판단한다.
