package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CartItemRequestDto {
	private final Long userId;
	private final Long productId;
	private final int quantity;
}
