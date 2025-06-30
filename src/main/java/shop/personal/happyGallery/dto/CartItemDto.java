package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.embeded.Money;

@AllArgsConstructor
@Getter
public class CartItemDto {
	private final Long productId;
	private final String name;
	private final Money price;
	private final int quantity;
}
