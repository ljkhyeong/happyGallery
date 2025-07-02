package shop.personal.happyGallery.model.embeded;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.personal.happyGallery.exception.ApplicationException;
import shop.personal.happyGallery.exception.ErrorCode;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Getter
@EqualsAndHashCode(of = {"currency", "amount"})
public class Money {
	private final Currency currency;
	@Column(precision = 19, scale = 2)
	private final BigDecimal amount;

	private Money(Currency currency, BigDecimal amount) {
		this.currency = currency;
		this.amount = amount.setScale(2, RoundingMode.HALF_UP);
	}


	public static Money of(long amount) {
		return new Money(Currency.getInstance("KRW"),
			BigDecimal.valueOf(amount));
	}

	public Money add(Money other) {
		verifyCurrency(other);
		return new Money(this.currency, amount.add(other.getAmount()));
	}

	public Money multiply(int factor) {
		if (factor <= 0) {
			throw new ApplicationException(ErrorCode.NOT_NEGATIVE_MONEY);
		}
		return new Money(this.currency, this.amount.multiply(BigDecimal.valueOf(factor)));
	}

	public void verifyCurrency(Money other) {
		if (!this.currency.equals(other.currency)) {
			throw new ApplicationException(ErrorCode.NOT_SAME_CURRENCY);
		}
	}

	// TODO 통화가 다르면 예외던져야함
	// 가격순으로 정렬하려면 Comparable 구현해야할수도

}
