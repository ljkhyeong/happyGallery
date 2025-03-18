package shop.personal.happyGallery.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderTest {

	@Test
	void addOrderItem_양방향관계설정() {
		// given
		Order order = Order.builder().build();
		OrderItem orderItem = OrderItem.builder().build();

		// when
		order.addOrderItem(orderItem);

		// then
		assertThat(order.getOrderItems()).contains(orderItem);
		assertThat(orderItem.getOrder()).isEqualTo(order);
	}

	@Test
	void calculateTotalPrice_총액계산() {

		//given
		Product p1 = Product.builder()
			.name("p1")
			.price(10000)
			.stock(10)
			.build();

		Product p2 = Product.builder()
			.name("p2")
			.price(5000)
			.stock(15)
			.build();

		OrderItem item1 = OrderItem.builder()
			.product(p1)
			.orderPrice(p1.getPrice())
			.quantity(2)
			.build();

		OrderItem item2 = OrderItem.builder()
			.product(p2)
			.orderPrice(p2.getPrice())
			.quantity(3)
			.build();

		Order order = Order.builder()
			.orderDate(LocalDateTime.now())
			.orderStatus(OrderStatus.PAYMENT_COMPLETE)
			.build();

		order.addOrderItem(item1);
		order.addOrderItem(item2);

		// when
		order.calculateTotalPrice();

		// then
		assertThat(order.getTotalPrice()).isEqualTo(35000);
	}

	@Test
	void setOrderStats_주문상태변경() {
		// given
		Order order = Order.builder().build();

		// when
		order.setOrderStatus(OrderStatus.IN_DELIVERY);

		// then
		assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.IN_DELIVERY);
	}

	@Test
	void cancelOrder_주문취소() {
		// given
		Order order = Order.builder()
			.orderStatus(OrderStatus.PAYMENT_COMPLETE)
			.build();

		// when
		order.cancelOrder();

		// then
		assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
	}
	@Test
	void cancelOrder_주문취소_배송중() {
		// given
		Order order = Order.builder()
			.orderStatus(OrderStatus.IN_DELIVERY)
			.build();

		// when
		// then
		assertThatThrownBy(() -> order.cancelOrder())
			.isInstanceOf(IllegalStateException.class);
	}

}
