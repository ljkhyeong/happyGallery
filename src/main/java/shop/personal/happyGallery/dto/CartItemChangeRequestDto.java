package shop.personal.happyGallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CartItemChangeRequestDto(
	Long userId,
	Long productId,
	@Max(1000)
	@Min(-1000)
	int quantity
) {
}
