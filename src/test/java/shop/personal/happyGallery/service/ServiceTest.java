package shop.personal.happyGallery.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import shop.personal.happyGallery.model.Cart;
import shop.personal.happyGallery.model.Order;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.repository.CartRepository;
import shop.personal.happyGallery.repository.OrderRepository;
import shop.personal.happyGallery.repository.ProductRepository;
import shop.personal.happyGallery.repository.UserRepository;

class ServiceTest {

	User user;
	Cart cart;
	Product product;

	@Mock
	UserRepository userRepository;
	@Mock
	CartRepository cartRepository;
	@Mock
	ProductRepository productRepository;
	@Mock
	OrderRepository orderRepository;

	@InjectMocks
	UserService userService;
	@InjectMocks
	CartService cartService;
	@InjectMocks
	ProductService productService;
	@InjectMocks
	OrderService orderService;

	@BeforeEach
	void setUp() {
		user = User.builder()
			.id(1L)
			.email("test@naver.com")
			.password("ascd")
			.build();

		cart = Cart.builder()
			.id(1L)
			.user(user)
			.build();

		product = Product.builder()
			.id(1L)
			.name("상품1")
			.description("이런상품")
			.price(10000)
			.stock(100)
			.build();
		MockitoAnnotations.openMocks(this);

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
	}

	@Nested
	class UserServiceTest {
	}

	@Nested
	class CartServiceTest {

		@Test
		@DisplayName("장바구니 상품 추가")
		void addItem() {
			// given
			// when
			cartService.addItem(user.getId(), product.getId(), 2);

			// then
			verify(productRepository).findById(product.getId());
			assertThat(cart.getItems()).hasSize(1);
			assertThat(cart.getItems().get(0).getProduct()).isEqualTo(product);
		}

		@Test
		@DisplayName("장바구니 상품 수량 변경")
		void changeQuantity() {
			// given
			cartService.addItem(user.getId(), product.getId(), 2);

			// when
			cartService.changeQuantity(user.getId(), product.getId(), 5);

			// then
			assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
		}

		@Test
		@DisplayName("장바구니 비우기")
		void clearCart() {
			// given
			cartService.addItem(user.getId(), product.getId(), 2);

			// when
			cartService.clearCart(user.getId());

			// then
			assertThat(cart.getItems()).hasSize(0);
		}


	}
}
