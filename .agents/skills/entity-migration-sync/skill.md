---
name: entity-migration-sync
description: Flyway and JPA schema synchronization workflow for happyGallery. Use when adding, removing, renaming, encrypting, or retyping entity fields, tables, columns, indexes, constraints, SQL migrations, or Java Flyway migrations, and when diagnosing Flyway/Hibernate schema failures. Include Java migrations when choosing the next version.
---

# happyGallery Entity And Migration Sync

## Current architecture

- SQL migrations: `bootstrap/src/main/resources/db/migration/`
- Java migrations: `bootstrap/src/main/java/com/personal/happygallery/bootstrap/migration/`
- JPA entities and repositories: `domain/` and `adapter-out-persistence/`
- Runtime database and integration tests: MySQL 8 through Testcontainers; do not design around H2 compatibility.
- As of 2026-07-18, SQL files end at V45 and Java migration `V46__ProtectPlaintextPersonalData` also exists.

## Rules

- Check both SQL and Java migration directories before selecting the next `V{n}`.
- Never edit an already applied migration. Add a new migration for corrections.
- Keep one coherent schema change per migration and plan data backfill before `NOT NULL`, uniqueness, FK, encryption, or column removal.
- Match DDL names, nullability, lengths, enum storage, indexes, and constraints with entity mappings and persistence queries.
- Search native queries, MyBatis XML, projections, fixtures, cleanup SQL, and REST Docs fixtures for every changed column/table.
- Use Java migrations only when transformation needs application crypto/value-object logic that SQL cannot safely express. Keep required keys and failure recovery explicit.
- Treat destructive production migrations and image rollback compatibility as operational decisions requiring an ADR or deployment note.

## Verification

- Migration/entity/transaction change: `./gradlew --no-daemon :application:useCaseTest`
- Focused migration logic: target the relevant migration test, such as `PersonalDataMigrationTest`, before widening.
- Compile persistence and bootstrap modules when a narrow structural check is sufficient.
- Update ADR-0022 for durable schema baselines and the relevant domain ADR/PRD when behavior changes.
