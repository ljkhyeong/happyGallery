# ADR-0031: Repository 쿼리 언어 선택 기준

**날짜**: 2026-05-05
**상태**: Accepted

---

## 왜 이 문서가 필요한가

`OrderRepository`는 일부 메서드를 `nativeQuery = true`로, `BookingRepository`는 모든 `@Query`를 JPQL + `JOIN FETCH`로 작성한다. 같은 카테고리(목록 조회·페이지네이션)인데 접근 방식이 다른 것처럼 보여 "왜 한쪽만 native인지", "통일해야 하지 않나"라는 질문이 반복된다.

실제로는 의도된 차이지만, 그 의도가 코드에는 없고 PR 리뷰 때마다 재생산되고 있어 기준 자체를 명문화한다.

---

## 결정

### 1. JPQL + `JOIN FETCH`가 기본이다

- 단건/목록 상세 조회는 JPQL `@Query`로 작성한다.
- 연관 엔티티가 필요하면 `JOIN FETCH`로 즉시 로딩해 N+1을 막는다 (예: `BookingRepository.findByUserIdWithDetails`).
- 단순 조건 메서드는 Spring Data derived query 그대로 둔다 (예: `findByStatusAndApprovalDeadlineAtBefore`).

### 2. native query는 다음 경우에만 사용한다

JPQL로 표현이 불가능하거나 옵티마이저가 복합 인덱스를 못 타는 경우로 한정한다.

- **커서 페이지네이션의 tuple comparison**
  `(created_at, id) < (:cursorCreatedAt, :cursorId)` 형태. JPQL은 row-value 비교를 지원하지 않아 이 패턴만 native 강제. 사례: `OrderRepository.findAllOrderByCreatedAtDescAfterCursor`.
- **지연 조인(deferred join) — 커버링 인덱스 서브쿼리로 ID만 구한 뒤 JOIN**
  OFFSET 페이지네이션이 깊어질 때 사용. 사례: `MyBatisAdminOrderSearchAdapter`의 검색 쿼리. (해당 패턴은 메모리 `feedback_deferred_join.md`와 일치)
- **DB 특화 함수/힌트**
  `FORCE INDEX`, MySQL 전용 윈도우 표현 등 ANSI/JPQL로 표현 불가능한 경우.

### 3. native query 작성 시 의무 사항

- 메서드 위 주석 한 줄에 *왜* native가 필요한지 적는다 (`tuple comparison으로 복합 인덱스 range scan 활용` 같은 식). 단순 "성능"만 적지 않는다.
- `:#{#status.name()}` 같은 enum binding을 쓸 때는 매핑 깨짐 위험을 고려해 `@Enumerated(EnumType.STRING)` 컬럼인지 확인한다.
- 가능하면 같은 메서드의 첫 페이지 변형은 JPQL로 두고(예: `findAllOrderByCreatedAtDesc`), 커서 이후만 native로 둔다 — 첫 페이지는 JPA caching/lifecycle을 거치는 편이 안전.

### 4. 통일 압력에 굴하지 않는다

`OrderRepository`와 `BookingRepository`의 비대칭은 의도다.
- `OrderRepository`: 관리자 검색에서 커서 페이지네이션이 필요해 native가 들어왔다.
- `BookingRepository`: 커서 페이지네이션 요건이 아직 없어 JPQL로 충분하다.

요건이 생기지 않은 곳을 "스타일 통일" 명목으로 native로 바꾸지 않는다. 반대로, 새 요건이 생기면 위 2번 기준에 해당할 때만 native로 간다.

---

## 결과

### 장점

- 같은 패턴의 PR 리뷰 질문이 반복되지 않는다.
- native가 있는 곳을 보면 "왜 거기만 native인지"를 코드/주석으로 추적 가능하다.

### 단점

- native query는 JPQL과 달리 컴파일 타임 검증이 약하다 — 컬럼 변경 시 실행 시점에야 깨지는 위험은 그대로 남는다. 이건 통합 테스트로 막는다(ADR-0026).
- 두 언어가 한 Repository 안에 섞일 수 있다 — 의도적이지만 가독성 비용은 인정.

---

## 참고 문서

- `docs/ADR/0026_통합_테스트_프로파일과_TestContainer_기준선/adr.md`
- `feedback_deferred_join.md` (메모리)
- `adapter-out-persistence/.../order/OrderRepository.java`
- `adapter-out-persistence/.../booking/BookingRepository.java`
- `adapter-out-persistence/.../dashboard/adapter/MyBatisAdminOrderSearchAdapter.java`
