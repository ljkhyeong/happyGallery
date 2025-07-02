package shop.personal.happyGallery.dto;

import static java.util.stream.Collectors.*;

import java.util.List;

import shop.personal.happyGallery.model.Order;
import shop.personal.happyGallery.model.enums.OrderStatus;
import shop.personal.happyGallery.model.embeded.Money;

public record OrderResponseDto(
	Long id,
	Long userId,
	OrderStatus orderStatus,
	List<ItemDto> items,
	Money totalPrice) {

	public static OrderResponseDto from(Order order) {
		return new OrderResponseDto(order.getId(), order.getUser().getId(), order.getOrderStatus(),
			order.getItems().stream()
				.map(i -> new ItemDto(i.getId(), i.getProduct().getName(), i.getProduct().getRealPrice(),
					i.getQuantity()))
				.collect(toUnmodifiableList()),
			order.getTotalPrice());
	}
}
