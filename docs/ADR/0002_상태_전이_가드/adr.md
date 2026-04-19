# ADR-0002: 상태 전이 가드 위치 및 예외 체계

- **상태**: 확정
- **날짜**: 2026-02-21
- **관련 파일**:
  - `domain/order/OrderStatus.java`
  - `domain/booking/SlotCapacity.java`
  - `domain/product/InventoryPolicy.java`
  - `domain/error/` (ErrorCode, HappyGalleryException, 개별 예외)
  - `adapter-in-web/.../GlobalExceptionHandler.java`

---

## 배경

상태 전이 실패(이미 환불된 주문 승인 시도, 정원 초과, 재고 부족)를
일관된 예외로 처리할 기반이 필요했다.
선택지는 (1) 서비스 레이어에서 직접 if-throw, (2) 도메인 객체에 가드 메서드, (3) 별도 정책 클래스였다.

---

## 결정

### 가드 위치: 도메인 객체/정책 클래스

| 대상 | 구현 위치 | 가드 메서드 |
|------|----------|------------|
| 주문 승인 가능 여부 | `OrderStatus.requireApprovable()` | 환불 상태이면 `AlreadyRefundedException` |
| 슬롯 정원 | `SlotCapacity.checkAvailable(int)` | `bookedCount >= 8`이면 `CapacityExceededException` |
| 재고 차감 | `InventoryPolicy.checkSufficient(int, int)` | `available < requested`이면 `InventoryNotEnoughException` |

### 예외 체계

```
RuntimeException
  └── HappyGalleryException(ErrorCode)   ← domain/error
        ├── AlreadyRefundedException      (409)
        ├── InventoryNotEnoughException   (409)
        ├── CapacityExceededException     (409)
        ├── RefundNotAllowedException     (422)
        ├── ChangeNotAllowedException     (422)
        ├── PassExpiredException          (422)
        └── NotFoundException             (404)
```

### 에러 응답 포맷 (고정)

```json
{ "code": "ALREADY_REFUNDED", "message": "이미 환불된 건입니다." }
```

- 필드: `code`(ErrorCode 이름), `message`(기본값 또는 오버라이드)
- `GlobalExceptionHandler`가 `HappyGalleryException` → `ErrorResponse`로 변환

---

## Alternatives

| 대안 | 기각 이유 |
|------|-----------|
| 서비스 레이어에서 if-throw | 검증 로직이 서비스 곳곳에 흩어지고 테스트가 서비스 단위에 종속됨 |
| 별도 `XxxValidator` 클래스 | 상태 판정은 도메인 지식이므로 도메인 객체와 분리 시 응집도 하락 |
| Spring `@ResponseStatus` 어노테이션 | 중앙 핸들러 없이는 응답 포맷 통일 불가 |

---

## 결과

**긍정**
- 상태 전이 가드가 도메인 객체에 응집 → 서비스 레이어 코드 단순화 예정
- `@Tag("policy")` 단위 테스트로 Spring 없이 빠르게 검증 가능
- 에러 응답 포맷이 단일 레코드로 고정 → 클라이언트 파싱 안정적

**부정 / 후속 작업**
- `OrderStatus.requireApprovable()`은 "승인 불가 상태 전체"를 커버하지 않음
  (예: 이미 `APPROVED_FULFILLMENT_PENDING` 상태에서 재승인 시도 → 별도 처리 필요)
- 서비스 레이어 구현 시 가드 호출 누락 방지를 코드 리뷰로 보완 필요

---

## References

- `docs/PRD/0001_기준_스펙/spec.md` § 3.1, § 4.1, § 12
- ADR-0001 (핵심 스키마 — 낙관적 락 컬럼)
