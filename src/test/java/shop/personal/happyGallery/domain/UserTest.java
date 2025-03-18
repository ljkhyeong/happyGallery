package shop.personal.happyGallery.domain;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserTest {

	@Test
	void addOrder_양방향관계설정() {
		// given
		User user = User.builder().build();
		Order order = Order.builder().build();

		// when
		user.addOrder(order);

		// then
		assertThat(user.getOrders()).contains(order);
		assertThat(order.getUser()).isEqualTo(user);
	}
}
