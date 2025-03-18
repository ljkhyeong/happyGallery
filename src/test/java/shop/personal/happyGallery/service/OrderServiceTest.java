package shop.personal.happyGallery.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import shop.personal.happyGallery.domain.Order;
import shop.personal.happyGallery.domain.Product;
import shop.personal.happyGallery.domain.User;
import shop.personal.happyGallery.repository.OrderRepository;
import shop.personal.happyGallery.repository.ProductRepository;
import shop.personal.happyGallery.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private ProductRepository productRepository;
	@Mock
	private OrderRepository orderRepository;

	@InjectMocks
	private OrderService orderService;

	@Test
	void createOrder_주문생성() {
		User user = User.builder().email("user1@gmail.com").build();
		Product product = spy(Product.builder().name("item1").stock(10).build());

		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(productRepository.findById(10L)).willReturn(Optional.of(product));
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		Order order = orderService.createOrder(1L, 10L, 2);
		
		verify(userRepository, times(1)).findById(1L);
		verify(productRepository, times(1)).findById(10L);
		verify(product, times(1)).reduceStock(2);
	}
}
