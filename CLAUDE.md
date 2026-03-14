# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# 빌드
./gradlew build

# 앱 실행 (local 프로필)
./gradlew :app:bootRun

# 전체 테스트
./gradlew test

# 단일 테스트 클래스
./gradlew :app:test --tests "com.personal.happygallery.SomeTest"

# DB 기동 (MySQL 8, docker-compose) — .env 파일 필요
docker compose up -d

# Health 확인
curl http://localhost:8080/actuator/health
```

## Architecture

멀티모듈 Gradle 프로젝트 (Spring Boot 4.0.2 / Java 21). 모듈 구조·레이어링 규칙은 `happygallery-spring-backend` 스킬 참조.

**핵심 도메인 열거형** (`domain` 모듈):
- `order`: OrderStatus, FulfillmentType, RefundStatus
- `booking`: BookingStatus, BalanceStatus, BookingHistoryAction
- `pass`: PassLedgerType
- `product`: ProductType, ProductStatus

**관측성**: `RequestIdFilter`가 `X-Request-Id` 헤더를 MDC에 주입하고 응답에 반환.
로그 패턴: `%5p [%X{requestId:-}]` (application.yml)

**DB 마이그레이션**: Flyway 자동 적용 (`classpath:db/migration`). 로컬 프로필은 MySQL(`localhost:3306/happygallery`), 환경변수 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`로 오버라이드 가능.

---

## 세션 시작 시
`HANDOFF.md`가 존재하면 가장 먼저 읽고 이전 컨텍스트를 이어받는다.

## 작업 시작 시 스킬 참조 규칙
1. 작업을 시작하기 전에 `.claude/skills/` 디렉토리에서 해당 작업 도메인과 매칭되는 스킬 파일을 읽는다.
2. 스킬의 **non-negotiable invariants** → **verification workflow** → **doc sync checklist** 순서를 따른다.
3. 주요 매핑:
   - 프론트 작업 → `happygallery-frontend-flows`
   - 주문 관련 → `happygallery-order-flows`
   - 예약 관련 → `happygallery-booking-flows`
   - 이용권 관련 → `happygallery-pass-flows`
   - 상품 관련 → `happygallery-product-flows`
   - 관리자 기능 → `happygallery-admin-flows`
   - API 계약 변경 → `api-contract`
   - DB 스키마 변경 → `db-schema-checklist`
   - 여러 모듈에 걸친 변경 → `happygallery-spring-backend`
4. 작업 완료 보고 시 실제 참조한 스킬 이름을 `사용: 스킬명` 형식으로 명시한다.

---

## Tool Usage Rules
- 파일 검색: `find` / `ls` 대신 **Glob** 도구 사용
- 파일 읽기: `cat` / `head` / `tail` 대신 **Read** 도구 사용
- 내용 검색: `grep` / `rg` 대신 **Grep** 도구 사용
- 파일 수정: `sed` / `awk` 대신 **Edit** 도구 사용
- Bash는 위 전용 도구로 대체할 수 없는 시스템 명령에만 사용한다.
- 위반 시 샌드박스 차단 → unsandboxed 팝업 발생

## Approval Minimization
- 사용자 확인이나 승인 요청은 기술적으로 꼭 필요한 경우에만 한다.
- 원격 쓰기, PR merge, 워크스페이스 밖 경로 쓰기처럼 실제 escalation이 필요한 작업만 승인 흐름을 사용한다.
- 이 저장소에서 반복적으로 샌드박스에 막힌 작업은 처음부터 unsandboxed/escalated 실행을 기본으로 한다. 대표적으로 Gradle JVM 명령, `gh pr create/view/merge/ready`, 원격 `git fetch/push/pull`, Docker 컨테이너 제어와 `docker exec`, Playwright 브라우저 설치/실행, 워크스페이스 밖 경로 또는 별도 worktree 쓰기가 여기에 포함된다.
- 위 작업들은 샌드박스에서 먼저 실패를 재현하거나 이유 설명용 질문을 추가하지 말고, 바로 권한 상승으로 처리한다.
- 가능한 한 현재 워크트리 안에서 해결하고, 보조 `status`/`log` 확인 때문에 승인 횟수를 늘리지 않는다.
- 사용자가 `바로`, `지금`, `곧바로`처럼 실행을 직접 지시한 경우에는, 파괴적이거나 모호한 상황이 아니면 재확인 질문 없이 진행한다.
- PR 작업에서는 먼저 mergeable 여부를 보고, 충돌이 없으면 바로 머지한다. 충돌이 있을 때만 필요한 파일만 정리한다.
- 사용자가 요구하지 않은 과한 재검증이나 중복 조회는 피하고, 변경 범위에 맞는 최소 검증만 수행한다.
- 이 저장소의 Gradle JVM 명령은 샌드박스에서 `FileLockContentionHandler` 소켓 제한으로 자주 실패하므로, `test`, `:app:test`, `:app:policyTest`, `:app:useCaseTest`, `:app:bootRun` 계열은 처음부터 unsandboxed/escalated 실행을 기본으로 한다.

## Skills (auto-trigger + explicit call)
- 상태머신/전이: `domain-state-machine`
- API 계약/오류/스키마: `api-contract`
- DB 스키마/마이그레이션: `db-schema-checklist`
- 작업 분해/Done 기준: `task-breakdown`
- JPA 엔티티↔Flyway DDL 동기화: `entity-migration-sync`
- 시간 경계 정책/Clock 주입: `time-boundary-policy`
