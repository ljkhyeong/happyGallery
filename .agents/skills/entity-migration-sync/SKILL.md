---
name: entity-migration-sync
description: happyGallery의 JPA 필드·테이블·컬럼·인덱스·제약·암호화 migration을 변경하거나 Flyway/schema 오류를 고칠 때 사용한다.
---

# happyGallery DB 변경

## 규칙

- migration 경로와 적용 파일 수정 금지는 `AGENTS.md`를 따른다. 테스트 DB도 MySQL Testcontainers이므로 H2 호환성을 추가하지 않는다.
- JPA 매핑, DDL, 쿼리의 이름·null 허용·길이·enum 저장·인덱스·제약을 맞춘다. native SQL, MyBatis XML, projection, fixture, 정리 SQL도 검색한다.
- `NOT NULL`·unique·FK·암호화·컬럼 제거 전에는 기존 데이터 처리 순서를 정한다. 한 migration은 하나의 변경 의도로 묶는다.
- 참조 컬럼 변경 전 FK를 확인한다. MySQL에서 필요하면 같은 원자적 `ALTER TABLE` 안에 FK 제거·재생성을 넣고 참조 동작을 보존한다. 제약 이름 재사용이 거부되면 새 이름을 쓴다.
- `NOT NULL`과 같은 조건의 `CHECK`는 추가하지 않는다. SQL로 처리하기 어려운 암호화·값 변환만 Java migration으로 작성하고 필요한 키와 복구 방법을 명시한다.
- 운영 데이터 삭제나 이미지 rollback 호환성이 달라지면 해당 ADR·배포 절차를 갱신한다.

## 검증

- 해당 migration 테스트 또는 `./gradlew --no-daemon :application:useCaseTest --tests "*대상클래스*"`부터 실행한다.
- 기존 테이블 제약 강화는 정상 적용과 잘못된 기존 데이터의 실패를 확인한다. 실패 시 컬럼·관련 제약이 일부만 변경되지 않아야 한다.
