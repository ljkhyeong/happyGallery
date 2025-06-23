package shop.personal.happyGallery.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.personal.happyGallery.exception.ApplicationException;
import shop.personal.happyGallery.exception.ErrorCode;
import shop.personal.happyGallery.model.embeded.BaseTimeEntity;
import shop.personal.happyGallery.model.embeded.Money;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Order extends BaseTimeEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Enumerated(EnumType.STRING)
	private OrderStatus orderStatus;
	private Money totalPrice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<OrderItem> items = new ArrayList<>();

	public static Order fromCart(Cart cart) {
		if(cart.getItems().isEmpty())
			throw new ApplicationException(ErrorCode.INVALID_ARGUMENT);

		Order newOrder = Order.builder()
							.user(cart.getUser())
							.orderStatus(OrderStatus.PLACED)
							.build();

		for (CartItem item : cart.getItems()) {
			newOrder.items.add(OrderItem.builder()
									.order(newOrder)
									.product(item.getProduct())
									.quantity(item.getQuantity())
									.price(item.getProduct().getPrice())
									.build());

			item.getProduct().decreaseStock(item.getQuantity());
		}

		newOrder.totalPrice = newOrder.items.stream()
			.map(OrderItem::calculateTotalPrice)
			.reduce(Money.of(0), Money::add);

		return newOrder;
	}



	public void prepareDelivering() { orderStatus = orderStatus.prepareDelivering(); }
	public void deliver() { orderStatus = orderStatus.deliver(); }
	public void delivered() { orderStatus = orderStatus.delivered(); }
	public void complete() { orderStatus = orderStatus.complete(); }





}
