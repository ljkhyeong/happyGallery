package shop.personal.happyGallery.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
@Builder
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private OrderStatus orderStatus;
	private LocalDateTime orderDate;
	private int totalPrice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<OrderItem> orderItems = new ArrayList<>();

	public void setUser(User user) {
		this.user = user;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public void addOrderItem(OrderItem orderItem) {
		this.orderItems.add(orderItem);
		orderItem.setOrder(this);
	}

	public int calculateTotalPrice() {
		return this.orderItems.stream()
			.mapToInt(OrderItem::getTotalPrice)
			.sum();
	}

	public void cancelOrder() {
		if (this.orderStatus == OrderStatus.IN_DELIVERY) {
			throw new IllegalStateException("배송중인 상품은 취소할 수 없습니다.");
		}

		setOrderStatus(OrderStatus.CANCELED);

		for (OrderItem item : this.orderItems) {
			Product product = item.getProduct();
			product.restoreStock(item.getQuantity());
		}
	}

}