package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.embeded.Money;

public record OrderItemDto(
	Long productId,
	String name,
	Money price,
	int quantity
) {
}
