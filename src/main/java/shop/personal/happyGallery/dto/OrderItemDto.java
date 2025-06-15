package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OrderItemDto {
	private final Long productId;
	private final String name;
	private final int price;
	private final int quantity;
}
