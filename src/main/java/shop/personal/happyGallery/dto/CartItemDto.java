package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.Product;

@AllArgsConstructor
@Getter
public class CartItemDto {
	private final Long productId;
	private final String name;
	private final int price;
	private final int quantity;
}
