package shop.personal.happyGallery.domain;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import shop.personal.happyGallery.model.Cart;
import shop.personal.happyGallery.model.Category;
import shop.personal.happyGallery.model.Order;
import shop.personal.happyGallery.model.OrderStatus;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.User;

class DomainModelTest {

	@Nested
	class UserTest {
		@Test
		@DisplayName("비밀번호 변경")
		void changePassword() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.build();

			// when
			user.changePassword("abcd2");

			// then
			assertThat(user.getPassword()).isEqualTo("abcd2");
		}

		@Test
		@DisplayName("주소 변경")
		void changeAddress() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			// when
			user.changeAddress("서울시");

			// then
			assertThat(user.getAddress()).isEqualTo("서울시");
		}
	}

	@Nested
	class CartTest {
		@Test
		@DisplayName("아이템 추가")
		void addItem() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			Product product2 = Product.builder()
				.name("물건2")
				.price(100000)
				.stock(100)
				.build();
			// when
			cart.addItem(product, 2);
			cart.addItem(product2, 5);

			// then
			assertThat(cart.getItems().size()).isEqualTo(2);
			assertThat(cart.getItems().get(0).getProduct()).isEqualTo(product);
		}
		@Test
		@DisplayName("같은 상품 추가 시 장바구니 내 같은 상품 수량 증가")
		void addItemMergesQuantity() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();
			// when
			cart.addItem(product, 2);
			cart.addItem(product, 3);

			// then
			assertThat(cart.getItems().size()).isEqualTo(1);
			assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
		}

		@Test
		@DisplayName("아이템 추가수량 0 이하일 시 예외 발생")
		void addItemQuantityValidation() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			// when
			// then
			assertThatThrownBy(() -> cart.addItem(product, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("아이템 제거")
		void removeItem() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			cart.addItem(product, 2);

			// when
			cart.changeQuantity(product, 0);

			// then
			assertThat(cart.getItems().size()).isEqualTo(0);
		}

		@Test
		@DisplayName("아이템 수량 변경")
		void changeQuantity() {

			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			cart.addItem(product, 2);

			// when
			cart.changeQuantity(product, 10);

			// then
			assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(10);
		}
		@Test
		@DisplayName("아이템 0이하로 수량 변경 시 카트에서 삭제")
		void changeQuantityZeroWithRemoveItem() {

			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			cart.addItem(product, 2);

			// when
			cart.changeQuantity(product, 0);

			// then
			assertThat(cart.getItems().size()).isEqualTo(0);
		}
		@Test
		@DisplayName("수량변경하려는 아이템이 카트에 없을 때 예외 발생")
		void changeQuantityWithoutCartItem() {

			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			Product product2 = Product.builder()
				.name("물건2")
				.price(100000)
				.stock(100)
				.build();

			cart.addItem(product, 2);

			// when
			// then
			assertThatThrownBy(() -> cart.changeQuantity(product2, 2))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class OrderTest {

		@Test
		@DisplayName("장바구니 물건으로 주문 생성")
		void fromCart() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			cart.addItem(product, 2);

			// when
			Order order = Order.fromCart(cart);

			// then
			assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PLACED);
			assertThat(order.getTotalPrice()).isEqualTo(product.getPrice() * 2);
		}

		@Test
		@DisplayName("빈 장바구니로 주문 생성 시 예외 발생")
		void fromCartWithoutCartItem() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			// when
			// then
			assertThatThrownBy(() -> Order.fromCart(cart))
				.isInstanceOf(IllegalStateException.class);
		}

		@Test
		@DisplayName("주문상태 변경")
		void changeOrderStatus() {
			// given
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

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
			User user = User.builder()
				.id(1L)
				.email("ljk@naver.com")
				.password("abcd")
				.address("충주시")
				.build();

			Cart cart = Cart.builder()
				.user(user)
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			cart.addItem(product, 2);
			Order order = Order.fromCart(cart);

			// when
			// then
			assertThatThrownBy(() -> order.complete())
				.isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@Nested
	class ProductTest {

		@Test
		@DisplayName("재고 증가")
		void increaseStock() {
			// given
			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			// when
			product.increaseStock(20);

			// then
			assertThat(product.getStock()).isEqualTo(30);
		}
		@Test
		@DisplayName("재고 증가 0이하, 1000초과일 시 예외발생")
		void increaseStockWithZero() {
			// given
			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			// when
			// then
			assertThatThrownBy(() -> product.increaseStock(0))
				.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> product.increaseStock(1001))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("재고 감소 0이하, 재고 이상의 수를 감소시킬 시 예외 발생")
		void decreaseStock() {
			// given
			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			// when
			// then
			assertThatThrownBy(() -> product.decreaseStock(0))
				.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> product.decreaseStock(11))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("카테고리 추가")
		void addCategory() {
			// given
			Category category = Category.builder()
				.name("의류")
				.build();

			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

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
