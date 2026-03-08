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

멀티모듈 Gradle 프로젝트 (Spring Boot 4.0.2 / Java 21).

```
app      ← 진입점. Web 레이어(Controller, Filter), Spring Boot main.
           common + domain + infra 모두 의존.
domain   ← 도메인 모델·열거형만. 외부 의존 없음(JPA API만 허용).
           common에 의존.
infra    ← DB, 외부 연동 구현체. (아직 초기)
common   ← 공통 유틸(시간, ID, 에러, 응답 포맷 등).
```

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

---

## Tool Usage Rules
- 파일 검색: `find` / `ls` 대신 **Glob** 도구 사용
- 파일 읽기: `cat` / `head` / `tail` 대신 **Read** 도구 사용
- 내용 검색: `grep` / `rg` 대신 **Grep** 도구 사용
- 파일 수정: `sed` / `awk` 대신 **Edit** 도구 사용
- Bash는 위 전용 도구로 대체할 수 없는 시스템 명령에만 사용한다.
- 위반 시 샌드박스 차단 → unsandboxed 팝업 발생

## Skills (auto-trigger + explicit call)
- 상태머신/전이: `domain-state-machine`
- API 계약/오류/스키마: `api-contract`
- DB 스키마/마이그레이션: `db-schema-checklist`
- 알림/환불/배치: `notification-refund-batch`
- 작업 분해/Done 기준: `task-breakdown`
- JPA 엔티티↔Flyway DDL 동기화: `entity-migration-sync`
- 시간 경계 정책/Clock 주입: `time-boundary-policy`
