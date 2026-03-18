# 금액 타입 도입 검토

## 현황

모든 금액 필드가 `long` / `BIGINT`로 통일되어 있다.

| 도메인 | 필드 | Java 타입 | DB 타입 |
|--------|------|-----------|---------|
| Order | totalAmount | long | BIGINT |
| OrderItem | unitPrice | long | BIGINT |
| Product | price | long | BIGINT |
| Booking | depositAmount, balanceAmount | long | BIGINT |
| BookingClass | price | long | BIGINT |
| Refund | amount | long | BIGINT |
| PassPurchase | totalPrice | long | BIGINT |

KRW 단일 통화 + 정수 연산만 사용하므로 정밀도 문제는 없다.

## 잠재적 문제

- 금액과 수량이 모두 `long`이라 컴파일 타임에 혼동을 잡을 수 없다 (예: `price * quantity` vs `price + quantity` 실수).
- 금액끼리의 통화 단위 불일치를 타입으로 방어하지 못한다.

## 선택지

### 1. 현행 유지 (`long`)

- KRW 단일 통화, 소수점 연산 없음 → 실용적으로 충분.
- 보일러플레이트 없음.

### 2. 도메인 VO 직접 정의

```java
public record Money(long amount) {
    public Money plus(Money other) { return new Money(amount + other.amount); }
    public Money times(int quantity) { return new Money(amount * quantity); }
}
```

- 외부 의존성 없이 타입 안전성 확보.
- JPA `@Embedded` 또는 `AttributeConverter`로 매핑.
- 다중 통화가 필요해지면 `Currency` 필드 추가.

### 3. 외부 라이브러리 (JavaMoney JSR 354 / Joda-Money)

- 다중 통화, 환율 변환, 통화별 rounding 정책이 필요한 금융 도메인에 적합.
- JPA 매핑(`AttributeConverter`), JSON 직렬화 커스터마이징 필요.
- DB 스키마도 `amount + currency` 두 컬럼으로 변경해야 의미 있음.
- 현재 규모 대비 도입 비용이 크다.

## 판단

현재는 **선택지 1 (long 유지)** 이 적절하다. 다중 통화가 필요해지면 **선택지 2 (도메인 VO)** 부터 도입한다. 외부 Money 라이브러리는 현재 규모에서는 JPA/JSON 매핑 비용 대비 이점이 적다.
