package shop.personal.happyGallery.dto;

import shop.personal.happyGallery.model.embeded.Money;

public record ItemDto(
	Long productId,
	String name,
	Money price,
	int quantity
) {
}
