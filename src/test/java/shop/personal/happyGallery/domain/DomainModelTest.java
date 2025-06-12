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

			Product product2 = Product.builder()
				.name("물건2")
				.price(100000)
				.stock(100)
				.build();
			// when
			cart.addItem(product, 2);
			cart.removeItem(product);

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

			Product product2 = Product.builder()
				.name("물건2")
				.price(100000)
				.stock(100)
				.build();
			// when
			cart.addItem(product, 2);
			cart.changeQuantity(product, 10);

			// then
			assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(10);
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
			assertThat(order.getItems().size()).isEqualTo(1);
			assertThat(order.getTotalPrice()).isEqualTo(product.getPrice() * 2);
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

			// when
			Order order = Order.fromCart(cart);
			order.prepareDelivering();

			// then
			assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERING_PREPARING);
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
		@DisplayName("재고 감소")
		void decreaseStock() {
			// given
			Product product = Product.builder()
				.name("물건1")
				.price(10000)
				.stock(10)
				.build();

			// when
			product.decreaseStock(9);

			// then
			assertThat(product.getStock()).isEqualTo(1);
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
