---
name: entity-migration-sync
description: >
  Checklist and conventions for Flyway DB migrations AND JPA entity sync in the happyGallery backend.
  Use this skill whenever a JPA entity field is added, removed, or renamed; a new Flyway migration script
  is needed; a column type or nullability changes; a new table or index is added; or you are reviewing
  migration safety before applying. This skill covers both the database schema side (migration file naming,
  SQL safety, H2 compatibility) and the JPA side (entity ↔ column alignment). Whenever you write or modify
  a Flyway migration script, change an @Entity or @Embeddable field, add a @Column, or ask "왜 Flyway가
  실패해요?" or "Hibernate validation error가 났어요", always use this skill first — it prevents the
  entity/schema drift that causes runtime Flyway or Hibernate validation failures. Also use this skill
  whenever you see H2 test failures after adding a column or changing a NOT NULL constraint.
---

# happyGallery DB Schema & Entity Sync

## Core references

- All migrations live in `app/src/main/resources/db/migration/`
- JPA entities: `infra/src/main/java/com/personal/happygallery/infra/`
- Domain value objects: `domain/src/main/java/com/personal/happygallery/domain/` (may have `@Embeddable`)
- Target DB: MySQL 8 (production and local), H2 `MODE=MySQL` (tests)
- Flyway applies migrations automatically on startup; no manual `migrate` needed

## Migration file conventions

```
V{n}__snake_case_description.sql
  예: V12__add_fulfillment_status_index.sql
```

- Always check the current highest `V{n}` before creating a new file.
- Never modify a migration that has already been applied (Flyway checksum validation will fail).
- Use only one concern per migration file; keep files small and focused.

## Cross-cutting rules

`happygallery-spring-backend`의 Respect module boundaries·Repository constraints·Test writing rules, `api-contract`의 Non-negotiable invariants, `happygallery-test-refactor`의 Assertion conventions를 이 스킬에서도 항상 따른다.

## Pre-apply checklist

**파일 명명:**
- [ ] 파일명이 `V{n+1}__snake_case_description.sql` 형태인가
- [ ] 다음 버전 번호를 사용했는가 (중복 없음)

**SQL 안전성:**
- [ ] `NOT NULL` 컬럼 추가 시 `DEFAULT` 값 또는 신규 테이블인가 (기존 row 오류 방지)
- [ ] 컬럼 타입 변경 시 데이터 손실 위험 검토했는가
- [ ] FK 추가 시 참조 테이블이 먼저 생성되어 있는가

**H2 호환성 (테스트):**
- [ ] `ENUM` 타입 사용 안 함 → `VARCHAR` 대체
- [ ] MySQL 전용 함수/문법 사용 안 함 (`REGEXP_REPLACE`, `JSON_*` 등 H2 미지원)
- [ ] `AUTO_INCREMENT` 또는 `BIGINT` 확인 (H2 MODE=MySQL에서 대부분 동작하나 복잡한 DDL 주의)

**인덱스:**
- [ ] 자주 필터링되는 컬럼에 인덱스 추가했는가
- [ ] FK 컬럼에 인덱스 있는가 (MySQL은 FK에 자동 인덱스 생성하지 않음)

**JPA 엔티티 동기화:**
- [ ] 변경된 컬럼명이 `@Column(name = "...")` 값과 정확히 일치하는가 (`snake_case`)
- [ ] `nullable` on `@Column` matches DDL (`NOT NULL` → `nullable = false`)
- [ ] 새 `@ManyToOne` / `@OneToMany` 에 matching FK column이 migration에 있는가
- [ ] 제거된 컬럼: entity 필드 제거 AND migration에서 컬럼 drop/null 처리 확인
- [ ] enum을 `@Enumerated(STRING)`으로 매핑하면 VARCHAR 길이가 충분한가

## Common pitfalls

- `ENUM` 타입을 MySQL에 쓰면 H2 테스트에서 파싱 오류 발생 → 항상 `VARCHAR`로 대체하고 CHECK 제약 또는 앱 레벨 validation 사용
- 기존 테이블에 `NOT NULL` 컬럼 추가 시 `DEFAULT` 없으면 MySQL도 에러 → 반드시 DEFAULT 또는 nullable=true로 시작 후 데이터 채우고 NOT NULL로 변경
- Flyway 체크섬: 이미 적용된 파일 수정하면 다음 기동 시 `FlywayException` 발생
- `snake_case` 컬럼명과 JPA 필드명 불일치: 반드시 `@Column(name = "...")` 명시

## Likely code locations

- `app/src/main/resources/db/migration/` — Migration scripts
- `app/src/main/resources/application.yml` — Flyway 설정 (spring.flyway.*)
- `infra/src/main/java/com/personal/happygallery/infra/booking/` — Booking JPA entities
- `infra/src/main/java/com/personal/happygallery/infra/order/` — Order JPA entities
- `infra/src/main/java/com/personal/happygallery/infra/pass/` — Pass JPA entities
- `infra/src/main/java/com/personal/happygallery/infra/product/` — Product/inventory JPA entities
- `infra/src/main/java/com/personal/happygallery/infra/payment/` — Payment log JPA entities
- `infra/src/main/java/com/personal/happygallery/infra/user/` — User JPA entities

## Verification workflow

- Migration + entity sync changes: `./gradlew --no-daemon :app:useCaseTest`
  (Flyway가 H2에 모든 migration 적용; entity/schema 불일치 즉시 실패)
- Schema apply only (no business logic): `./gradlew :app:test --tests "*FlywayMigration*"` (해당 테스트 있으면), 없으면 `useCaseTest`

## Doc sync checklist

- 스키마 변경이 API 동작에 영향: `docs/PRD/0001_기준_스펙/spec.md`
- 중요한 스키마 설계 결정: 해당 ADR 또는 신규 ADR 작성
- Session status: `HANDOFF.md`
