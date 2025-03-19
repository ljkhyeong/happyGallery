package shop.personal.happyGallery.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.domain.Order;
import shop.personal.happyGallery.domain.OrderItem;
import shop.personal.happyGallery.domain.OrderStatus;
import shop.personal.happyGallery.domain.Product;
import shop.personal.happyGallery.domain.User;
import shop.personal.happyGallery.repository.OrderRepository;
import shop.personal.happyGallery.repository.ProductRepository;
import shop.personal.happyGallery.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final ProductRepository productRepository;

	public Order createOrder(Long userId, Long productId, int quantity) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저"));

		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품"));

		product.reduceStock(quantity);

		// 결제완료 로직 위에 있어야할듯
		Order order = Order.builder()
			.user(user)
			.orderDate(LocalDateTime.now())
			.orderStatus(OrderStatus.PAYMENT_COMPLETE)
			.build();

		OrderItem orderItem = OrderItem.builder()
			.product(product)
			.orderPrice(product.getPrice())
			.quantity(quantity)
			.build();
		order.addOrderItem(orderItem);

		order.calculateTotalPrice();

		return orderRepository.save(order);
	}
}
