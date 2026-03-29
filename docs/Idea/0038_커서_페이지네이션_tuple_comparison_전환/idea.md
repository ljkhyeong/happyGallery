# 0038. 커서 페이지네이션 OR → Tuple Comparison 전환

- **상태**: 적용 완료
- **일자**: 2026-03-28

---

## 배경

커서 기반 페이지네이션에서 동점(같은 `created_at`) 처리를 위해 `(timestamp, id)` 복합 키를 사용한다.
기존 JPQL 구현은 이를 OR 조건으로 풀어 썼다:

```sql
WHERE created_at < :cursorCreatedAt
   OR (created_at = :cursorCreatedAt AND id < :cursorId)
```

## 문제

MySQL 옵티마이저는 OR의 좌·우 조건을 독립된 범위로 취급한다.
`(created_at DESC, id DESC)` 복합 인덱스가 있어도 단일 range scan으로 처리하지 못하고,
`index_merge` 또는 풀스캔으로 빠질 수 있다.

## 해결

MySQL 8의 **row constructor comparison**은 복합 인덱스의 B-Tree 순서를 그대로 활용한다:

```sql
WHERE (created_at, id) < (:cursorCreatedAt, :cursorId)
```

EXPLAIN 결과가 `type: range`으로 나오며, OR 방식 대비 데이터 양이 클수록 성능 차이가 커진다.

## 적용 내용

| 파일 | 변경 |
|------|------|
| `OrderRepository.java` | 커서 쿼리 2건을 JPQL → native query + tuple comparison으로 전환 |
| `V29__add_cursor_pagination_indexes.sql` | `(created_at DESC, id DESC)`, `(status, created_at DESC, id DESC)` 복합 인덱스 추가 |

## 주의사항

### JPQL 미지원
JPQL은 tuple comparison `(a, b) < (x, y)` 문법을 지원하지 않는다.
따라서 `nativeQuery = true`로 전환이 필요하다.

### native query에서 enum 파라미터
native query에 `OrderStatus` enum을 직접 바인딩하면 타입 오류가 발생할 수 있다.
SpEL 표현식 `:#{#status.name()}`으로 문자열 변환한다.

### SELECT * 회피
native query에서 `SELECT *`를 쓰면 엔티티에 매핑되지 않은 DB 컬럼(예: `bundle_id`)이
포함되어 Hibernate 매핑에서 예기치 못한 문제가 발생할 수 있다.
엔티티 필드와 일치하는 컬럼만 명시적으로 열거한다.

## 향후 적용 기준

새로운 커서 페이지네이션을 추가할 때 아래 규칙을 따른다:
1. **tuple comparison** `(a, b) < (?, ?)` 사용 (OR 금지)
2. **native query** + 컬럼 명시적 열거
3. **복합 인덱스** `(정렬키 DESC, id DESC)` 선행 생성
4. enum 파라미터는 SpEL `:#{#param.name()}`으로 변환
