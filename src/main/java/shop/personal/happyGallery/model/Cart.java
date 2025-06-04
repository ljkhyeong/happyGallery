package shop.personal.happyGallery.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Cart {

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<CartItem> items = new ArrayList<>();

	public void addItem(Product product, int quantity) {
		if (quantity <= 0)
			throw new IllegalArgumentException("1개 이상 입력해주세요.");
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

	public void removeItem(Product product) {
		items.removeIf(item -> item.getProduct().equals(product));
	}

	public void changeQuantity(Product product, int quantity) {
		// TODO quantity를 정수 범위를 넘어서면 오버플로우가 발생할수도
		if (quantity <= 0) {
			removeItem(product);
			return;
		}

		CartItem itemInCart = items.stream()
			.filter(item -> item.getProduct().equals(product))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("해당하는 아이템이 카트에 존재하지 않습니다."));

		itemInCart.changeQuantity(quantity);
	}
	
}
