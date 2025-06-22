package shop.personal.happyGallery.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import shop.personal.happyGallery.model.embeded.BaseTimeEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Cart extends BaseTimeEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<CartItem> items = new ArrayList<>();

	public void setUser(User user) {
		this.user = user;
	}

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

	public void removeItem(Product product) {
		items.removeIf(item -> item.getProduct().equals(product));
	}

	public void clear() {
		items.clear();
	}

}
