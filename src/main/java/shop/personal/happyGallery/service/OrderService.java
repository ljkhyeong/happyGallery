package shop.personal.happyGallery.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.OrderResponseDto;
import shop.personal.happyGallery.model.Cart;
import shop.personal.happyGallery.model.Order;
import shop.personal.happyGallery.model.OrderItem;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.repository.CartRepository;
import shop.personal.happyGallery.repository.OrderRepository;
import shop.personal.happyGallery.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final UserRepository userRepository;
	private final CartRepository cartRepository;
	private final OrderRepository orderRepository;

	@Transactional(readOnly = true)
	public OrderResponseDto getOrderInfo(Long orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 주문 없음"));
		return OrderResponseDto.from(order);
	}

	@Transactional
	public OrderResponseDto createOrderFromCart(Long userId) {
		Cart cart = cartRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 카트 없음"));

		Order newOrder = Order.fromCart(cart);
		orderRepository.save(newOrder);

		return OrderResponseDto.from(newOrder);
	}
}
