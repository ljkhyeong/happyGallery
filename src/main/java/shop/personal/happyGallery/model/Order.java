package shop.personal.happyGallery.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
public class Order {

	@Id
	@GeneratedValue
	private Long id;
	@Enumerated(EnumType.STRING)
	private OrderStatus orderStatus;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<OrderItem> items = new ArrayList<>();

	public Order fromCart(Cart cart) {
		if(cart.getItems().isEmpty())
			throw new IllegalStateException("장바구니가 비어있습니다.");

		Order newOrder = Order.builder()
							.user(cart.getUser())
							.orderStatus(OrderStatus.PLACED)
							.build();

		for (CartItem item : cart.getItems()) {
			newOrder.items.add(OrderItem.builder()
									.order(newOrder)
									.product(item.getProduct())
									.quantity(item.getQuantity())
									.build());
		}

		return newOrder;
	}


}
