package shop.personal.happyGallery.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.personal.happyGallery.exception.ApplicationException;
import shop.personal.happyGallery.exception.ErrorCode;
import shop.personal.happyGallery.model.embeded.BaseTimeEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Cart extends BaseTimeEntity{

	private static final int MAX_QUANTITY = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<CartItem> items = new HashSet<>();

	public void setUser(User user) {
		this.user = user;
	}

	public void addItem(Product product, int quantity) {
		if (quantity <= 0)
			throw new ApplicationException(ErrorCode.NOT_NEGATIVE_CARTITEM_QUANTITY);
		for (CartItem item : items) {
			if (item.getProduct().equals(product)) {
				item.changeQuantity(item.getQuantity() + quantity);
				return;
			}
		}
		items.add(CartItem.builder()
							.cart(this)
							.product(product)
							.quantity(quantity)
							.build());
	}

	public void changeQuantity(Product product, int quantity) {
		if (quantity >= MAX_QUANTITY) {
			throw new ApplicationException(ErrorCode.NOT_OVER_THOUSAND_CARTITEM_QUANTITY);
		}
		if (quantity <= 0) {
			removeItem(product);
			return;
		}

		CartItem itemInCart = items.stream()
			.filter(item -> item.getProduct().equals(product))
			.findFirst()
			.orElseThrow(() -> new ApplicationException(ErrorCode.NOT_EXISTS_ITEM_IN_CART));

		itemInCart.changeQuantity(quantity);
	}

	public void removeItem(Product product) {
		items.removeIf(item -> item.getProduct().equals(product));
	}

	public void clear() {
		items.clear();
	}

}
