package shop.personal.happyGallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.embeded.Money;

public record ProductRegisterRequestDto(
	@NotNull
	String name,
	String description,
	@NotNull
	Money price,
	@Positive
	@Max(1000)
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
