# JSON + Generated Column 도입 검토 메모

**날짜**: 2026-03-06  
**상태**: Backlog (추후 검토)

---

## 배경

가변 속성(옵션/메타데이터)이 늘어날 때,

- `option1`, `option2` 같은 확장 불가능한 컬럼 증가
- 과도한 정규화로 인한 조인 복잡도 증가
- JSON 단일 컬럼 사용 시 검색 성능 저하

가 반복될 수 있다.

현재 happyGallery 스키마는 정규화 중심이며 JSON 컬럼은 사용하지 않는다.  
따라서 **즉시 적용 대상은 없지만**, 향후 가변 속성이 생길 때 표준 가이드를 선반영한다.

---

## 적용 트리거

아래 중 하나라도 충족하면 도입 검토를 시작한다.

1. 특정 도메인에서 키 구조가 자주 바뀌는 속성이 발생
2. 옵션 필드 확장 요청이 반복되어 스키마 변경 비용이 커짐
3. JSON 내부 키 기준 조회/집계 요구가 운영 쿼리로 승격

---

## 권장 패턴 (MySQL 8 기준)

1. 원본은 JSON 컬럼에 저장
2. 조회 조건으로 자주 쓰는 키는 Generated Column으로 노출
3. Generated Column에 인덱스 부여

예시:

```sql
ALTER TABLE example_events
  ADD COLUMN payload JSON NOT NULL,
  ADD COLUMN event_type_v VARCHAR(30)
    GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload, '$.eventType'))) STORED,
  ADD INDEX idx_example_events_event_type_v (event_type_v);
```

---

## 주의사항

1. 조회 조건으로 자주 쓰지 않는 키까지 Generated Column을 남발하지 않는다.
2. JSON 스키마 버전(`schemaVersion`)을 payload에 두어 하위 호환을 관리한다.
3. 기존 컬럼에서 JSON으로 이관 시 Flyway로 단계적 마이그레이션한다.

---

## 현재 결론

현 시점에는 기존 정규화 모델 유지가 타당하다.  
다만 향후 가변 속성 도입 시에는 본 문서의 패턴을 기본안으로 검토한다.
