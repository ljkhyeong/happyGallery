package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.embeded.Money;

public record CartItemDto(
	Long productId,
	String name,
	Money price,
	int quantity
) {
}
