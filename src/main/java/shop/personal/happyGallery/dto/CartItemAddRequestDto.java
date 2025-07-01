package shop.personal.happyGallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record CartItemAddRequestDto(
	Long userId,
	Long productId,
	@Positive
	@Max(1000)
	int quantity
) {
}
