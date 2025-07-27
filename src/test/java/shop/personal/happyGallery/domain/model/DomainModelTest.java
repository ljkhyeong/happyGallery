package shop.personal.happyGallery.domain.model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import shop.personal.happyGallery.exception.ApplicationException;
import shop.personal.happyGallery.exception.ErrorCode;
import shop.personal.happyGallery.model.Cart;
import shop.personal.happyGallery.model.CartItem;
import shop.personal.happyGallery.model.Category;
import shop.personal.happyGallery.model.Order;
import shop.personal.happyGallery.model.enums.OrderStatus;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.model.embeded.Address;
import shop.personal.happyGallery.model.embeded.Money;
import shop.personal.happyGallery.model.embeded.PhoneNumber;

class DomainModelTest {

	class TestFixture {
		public static User.UserBuilder user() {
			return User.builder()
				.email("test@test.com")
				.address(new Address("testCity", "testZipCode", "testStreet", "testEtc"))
				.phoneNumber(new PhoneNumber("82", "1011112222"));
		}

		public static Product.ProductBuilder product() {
			return Product.builder()
				.name("테스트상품")
				.description("테스트용")
				.price(Money.of(10000))
				.realPrice(Money.of(10000))
				.stock(100);
		}
	}

	@BeforeEach
	public void init() {
	}


	@Nested
	class UserTest {



		@Test
		@DisplayName("비밀번호 변경")
		void changePassword() {
			// given
			User user = TestFixture.user().build();
			PasswordEncoder encoder = mock(PasswordEncoder.class);
			when(encoder.encode(anyString())).thenReturn("encodedPassword");

			// when
			user.changePassword(encoder, "newPassword");

			// then
			assertThat(user.getPasswordHash()).isEqualTo(encoder.encode("encodedPassword"));
		}

		@Test
		@DisplayName("주소 변경")
		void changeAddress() {
			// given
			User user = TestFixture.user().build();

			// when
			user.changeAddress(new Address("서울시", "zipcode", "street", "etc"));

			// then
			assertThat(user.getAddress().getCity()).isEqualTo("서울시");
		}
	}

	@Nested
	class CartTest {
		@Test
		@DisplayName("아이템 추가")
		void addItem() {
			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = TestFixture.product().name("물건1").build();

			Product product2 = TestFixture.product().name("물건2").build();
			// when
			cart.addItem(product, 5);
			cart.addItem(product2, 10);

			// then
			assertThat(cart.getItems().size()).isEqualTo(2);
		}
		@Test
		@DisplayName("같은 상품 추가 시 장바구니 내 같은 상품 수량 증가")
		void addItemMergesQuantity() {
			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = TestFixture.product().build();

			// when
			cart.addItem(product, 2);
			cart.addItem(product, 3);

			// then
			assertThat(cart.getItems().size()).isEqualTo(1);
			assertThat(cart.getItems().stream()
				.filter((item) -> item.equals(CartItem.builder().cart(cart).product(product).build()))
				.findFirst().get().getQuantity()).isEqualTo(5);
		}

		@ParameterizedTest
		@ValueSource(ints = {0, -1})
		@DisplayName("아이템 추가수량 0 이하일 시 예외 발생")
		void addItemQuantityValidation(int qty) {
			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = TestFixture.product().build();

			// when
			// then
			assertThatThrownBy(() -> cart.addItem(product, qty))
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.NOT_NEGATIVE_CARTITEM_QUANTITY.getMessage());
		}

		@Test
		@DisplayName("아이템 제거")
		void removeItem() {
			// given
			User user = TestFixture.user().build();
			Cart cart = Cart.builder()
				.user(user)
				.build();
			Product product = TestFixture.product().build();
			cart.addItem(product, 2);

			// when
			cart.removeItem(product);

			// then
			assertThat(cart.getItems().size()).isEqualTo(0);
		}

		@Test
		@DisplayName("아이템 수량 변경")
		void changeQuantity() {
			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = TestFixture.product().build();

			cart.addItem(product, 2);

			// when
			cart.changeQuantity(product, 10);

			// then
			assertThat(cart.getItems().stream()
				.filter((item) -> item.equals(CartItem.builder().cart(cart).product(product).build()))
				.findFirst().get().getQuantity()).isEqualTo(10);
		}
		@ParameterizedTest
		@ValueSource(ints = {0, -1})
		@DisplayName("아이템 0이하로 수량 변경 시 카트에서 삭제")
		void changeQuantityZeroWithRemoveItem(int qty) {

			// given
			User user = TestFixture.user().build();
			Cart cart = Cart.builder()
				.user(user)
				.build();
			Product product = TestFixture.product().build();
			cart.addItem(product, 2);

			// when
			cart.changeQuantity(product, qty);

			// then
			assertThat(cart.getItems().size()).isEqualTo(0);
		}
		@Test
		@DisplayName("수량 변경하려는 아이템이 카트에 없을 때 예외 발생")
		void changeQuantityWithoutCartItem() {

			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = TestFixture.product()
				.name("상품1").build();

			Product product2 = TestFixture.product()
				.name("상품2").build();

			cart.addItem(product2, 2);

			// when
			// then
			assertThatThrownBy(() -> cart.changeQuantity(product, 1))
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.NOT_EXISTS_ITEM_IN_CART.getMessage());
		}
	}

	@Nested
	class OrderTest {

		@Test
		@DisplayName("장바구니 물건으로 주문 생성")
		void fromCart() {
			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = TestFixture.product().build();

			cart.addItem(product, 2);

			// when
			Order order = Order.fromCart(cart);

			// then
			assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PLACED);
			assertThat(order.getUser()).isEqualTo(user);
			assertThat(order.getTotalPrice())
				.isEqualTo(Money.of(product.getPrice().getAmount().multiply(BigDecimal.valueOf(2)).longValue()));
		}

		@Test
		@DisplayName("빈 장바구니로 주문 생성 시 예외 발생")
		void fromCartWithoutCartItem() {
			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			// when
			// then
			assertThatThrownBy(() -> Order.fromCart(cart))
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.EMPTY_CART.getMessage());
		}

		@Test
		@DisplayName("주문상태 변경")
		void changeOrderStatus() {
			// given
			User user = TestFixture.user().build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = TestFixture.product().build();

			cart.addItem(product, 2);
			Order order = Order.fromCart(cart);

			// when
			order.prepareDelivering();
			order.deliver();

			// then
			assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERING);
		}
		@Test
		@DisplayName("잘못된 주문상태 변경 시 예외 발생")
		void changeIncorrectOrderStatus() {
			// given
			User user = TestFixture.user().build();
			Cart cart = Cart.builder()
				.user(user)
				.build();
			Product product = TestFixture.product().build();
			cart.addItem(product, 2);
			Order order = Order.fromCart(cart);

			// when
			// then
			assertThatThrownBy(() -> order.complete())
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.INVALID_OPERATION_ORDERSTATUS.getMessage());
		}
	}

	@Nested
	class ProductTest {

		@Test
		@DisplayName("재고 증가")
		void increaseStock() {
			// given
			Product product = TestFixture.product().build();
			int defaultStock = product.getStock();

			// when
			product.increaseStock(20);

			// then
			assertThat(product.getStock()).isEqualTo(defaultStock + 20);
		}
		@ParameterizedTest
		@ValueSource(ints = {0, -1})
		@DisplayName("재고 증가를 음수로 할 시 예외발생")
		void increaseStockWithZero(int qty) {
			// given
			Product product = TestFixture.product().build();

			// when
			// then
			assertThatThrownBy(() -> product.increaseStock(qty))
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.NOT_NEGATIVE_STOCK.getMessage());
		}

		@ParameterizedTest
		@ValueSource(ints = {1001, 1002})
		@DisplayName("재고 증가를 1000 초과 할 시 예외발생")
		void increaseStockWithOverThousand(int qty) {
			// given
			Product product = TestFixture.product().build();

			// when
			// then
			assertThatThrownBy(() -> product.increaseStock(qty))
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.NOT_OVER_THOUSAND_STOCK.getMessage());
		}

		@ParameterizedTest
		@ValueSource(ints = {0, -1})
		@DisplayName("재고 감소 수량 0이하를 시킬 시 예외 발생")
		void decreaseStock(int qty) {
			// given
			Product product = TestFixture.product().build();

			// when
			// then
			assertThatThrownBy(() -> product.decreaseStock(qty))
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.NOT_NEGATIVE_STOCK.getMessage());
		}

		@Test
		@DisplayName("재고 이상의 수를 감소시킬 시 예외 발생")
		void decreaseStock() {
			// given
			Product product = TestFixture.product().build();

			// when
			// then
			assertThatThrownBy(() -> product.decreaseStock(Integer.MAX_VALUE))
				.isInstanceOf(ApplicationException.class)
				.hasMessageContaining(ErrorCode.NOT_DECREASE_OVER_STOCK.getMessage());
		}

		@Test
		@DisplayName("카테고리 추가")
		void addCategory() {
			// given
			Category category = Category.builder()
				.name("의류")
				.build();

			Product product = TestFixture.product().build();

			// when
			product.addCategory(category);

			// then
			assertThat(category.getProducts().contains(product)).isTrue();
			assertThat(product.getCategory()).isEqualTo(category);
		}
	}

	@Nested
	class CategoryTest {
		@Test
		@DisplayName("카테고리 자식 추가")
		void addChilderen() {
			// given
			Category category = Category.builder()
				.name("의류")
				.build();

			Category categoryChild = Category.builder()
				.name("바지")
				.build();

			// when
			category.addChildren(categoryChild);

			// then
			assertThat(category.getChildren().contains(categoryChild)).isTrue();
			assertThat(categoryChild.getParent()).isEqualTo(category);
		}
	}

}
