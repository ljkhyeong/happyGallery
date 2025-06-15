package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
public class CartItemRequestDto {
	private Long userId;
	private Long productId;
	private int quantity;
}
