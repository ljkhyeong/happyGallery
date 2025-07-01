package shop.personal.happyGallery.dto;

import static java.util.stream.Collectors.*;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.Cart;

public record CartResponseDto(
	Long id,
	Long userId,
	List<ItemDto> items
) {

	public static CartResponseDto from(Cart cart) {
		return new CartResponseDto(cart.getId(),
			cart.getUser().getId(),
			cart.getItems().stream()
				.map(i -> new ItemDto(i.getProduct().getId(),
					i.getProduct().getName(),
					i.getProduct().getRealPrice(),
					i.getQuantity()))
				.collect(toUnmodifiableList()));
	}
}
