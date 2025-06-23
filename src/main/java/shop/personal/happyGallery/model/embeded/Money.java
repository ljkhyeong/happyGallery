package shop.personal.happyGallery.model.embeded;

import java.math.BigDecimal;
import java.util.Currency;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(of = {"currency", "amount"})
public class Money {
	private final Currency currency;
	private final BigDecimal amount;

	public static Money of(long amount) {
		return new Money(Currency.getInstance("KRW"), BigDecimal.valueOf(amount));
	}

	public Money add(Money money) {
		return new Money(Currency.getInstance("KRW"), amount.add(money.getAmount()));
	}

	public Money multiply(int factor) {
		return new Money(Currency.getInstance("KRW"), this.amount.multiply(BigDecimal.valueOf(factor)));
	}

	// TODO 통화가 다르면 예외던져야함
	// 가격순으로 정렬하려면 Comparable 구현해야할수도

}
