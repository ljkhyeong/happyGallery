package shop.personal.happyGallery.model;

import jakarta.persistence.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.personal.happyGallery.exception.ApplicationException;
import shop.personal.happyGallery.exception.ErrorCode;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
@Table(
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"cart_id", "product_id"})
	}
)
public class CartItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private int quantity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cart_id")
	private Cart cart;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	public void changeQuantity(int quantity) {
		if (quantity <= 0)
			throw new ApplicationException(ErrorCode.INVALID_ARGUMENT);
		this.quantity = quantity;
	}
}
