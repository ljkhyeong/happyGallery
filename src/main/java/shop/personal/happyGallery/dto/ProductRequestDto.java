package shop.personal.happyGallery.dto;

import lombok.Data;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.embeded.Money;

@Data
public class ProductRequestDto {
	private String name;
	private String description;
	private Money price;
	private int stock;

	public Product toEntity() {
		return Product.builder()
			.name(name)
			.description(description)
			.price(price)
			.realPrice(price)
			.stock(stock)
			.build();
	}
}
