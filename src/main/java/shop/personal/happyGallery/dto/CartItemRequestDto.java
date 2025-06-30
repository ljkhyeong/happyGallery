package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;


public record CartItemRequestDto(
	Long userId,
	Long productId,
	int quantity
) {}
