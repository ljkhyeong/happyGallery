package shop.personal.happyGallery.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import shop.personal.happyGallery.dto.OrderResponseDto;
import shop.personal.happyGallery.dto.ProductResponseDto;
import shop.personal.happyGallery.model.Cart;
import shop.personal.happyGallery.model.Category;
import shop.personal.happyGallery.model.enums.OrderStatus;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.model.embeded.Money;
import shop.personal.happyGallery.repository.CartRepository;
import shop.personal.happyGallery.repository.OrderRepository;
import shop.personal.happyGallery.repository.ProductRepository;
import shop.personal.happyGallery.repository.UserRepository;

class ServiceTest {

	User user;
	Cart cart;
	Product product;
	Category category;

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
			.passwordHash("ascd")
			.build();

		cart = Cart.builder()
			.id(1L)
			.user(user)
			.build();

		product = Product.builder()
			.id(1L)
			.name("상품1")
			.description("이런상품")
			.realPrice(Money.of(10000))
			.price(Money.of(10000))
			.stock(100)
			.build();

		category = Category.builder()
			.id(1L)
			.name("의류")
			.build();

		MockitoAnnotations.openMocks(this);

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
		when(productRepository.findByCategoryId(category.getId())).thenReturn(List.of(product));
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
		}

		@Test
		@DisplayName("장바구니 상품 수량 변경")
		void changeQuantity() {
			// given
			cart.addItem(product, 2);

			// when
			cartService.changeQuantity(user.getId(), product.getId(), 5);

			// then
			assertThat(cart.getItems().stream().findFirst().get().getQuantity()).isEqualTo(5);
		}

		@Test
		@DisplayName("장바구니 비우기")
		void clearCart() {
			// given
			cart.addItem(product, 2);

			// when
			cartService.clearCart(user.getId());

			// then
			assertThat(cart.getItems()).hasSize(0);
		}
	}

	@Nested
	class OrderServiceTest {

		@Test
		@DisplayName("장바구니로부터 주문 생성")
		void createOrderFromCart() {
			// given
			cart.addItem(product, 2);
			// TODO : addItem 더 쉽게 하게 fixture 수정해야할듯

			// when
			OrderResponseDto responseDto = orderService.createOrderFromCart(user.getId());

			// then
			assertThat(responseDto.userId()).isEqualTo(user.getId());
			assertThat(responseDto.orderStatus()).isEqualTo(OrderStatus.PLACED);
			assertThat(responseDto.totalPrice()).isEqualTo(Money.of(product.getRealPrice().getAmount().longValue() * 2));
			assertThat(product.getStock()).isEqualTo(98);
		}
	}

	@Nested
	class ProductServiceTest {
		@Test
		@DisplayName("카테고리 해당 아이템 가져오기")
		void getCategoryProducts() {
			// given
			product.addCategory(category);

			// when
			List<ProductResponseDto> categoryProducts = productService.getCategoryProducts(category.getId());

			// then
			assertThat(categoryProducts.get(0).id()).isEqualTo(1L);
			assertThat(categoryProducts.get(0).name()).isEqualTo("상품1");
		}
	}

}
