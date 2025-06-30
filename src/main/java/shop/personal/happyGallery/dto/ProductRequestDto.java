package shop.personal.happyGallery.dto;

import lombok.Data;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.embeded.Money;

public record ProductRequestDto(
	String name,
	String description,
	Money price,
	int stock) {

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
