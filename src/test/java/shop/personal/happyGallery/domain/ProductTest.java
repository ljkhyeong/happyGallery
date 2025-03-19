package shop.personal.happyGallery.domain;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTest {

	private Product product;
	@BeforeEach
	void createProduct() {
		product = Product.builder()
			.name("test product")
			.stock(10)
			.price(10000)
			.build();
	}

	@Test
	void reduceStock_재고차감() {
		product.reduceStock(3);

		assertThat(product.getStock()).isEqualTo(7);
	}

	@Test
	void reduceStock_재고차감_재고부족() {

		assertThatThrownBy(() -> product.reduceStock(11))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("재고 부족");
	}

}
