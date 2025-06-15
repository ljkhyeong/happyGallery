package shop.personal.happyGallery.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import shop.personal.happyGallery.model.Order;
import shop.personal.happyGallery.model.OrderItem;
import shop.personal.happyGallery.model.OrderStatus;
import shop.personal.happyGallery.model.User;

@AllArgsConstructor
@Getter
public class OrderResponseDto {
	private Long id;
	private Long userId;
	private OrderStatus orderStatus;
	private final List<OrderItemDto> items;
	private int totalPrice;

	public static OrderResponseDto from(Order order) {
		return new OrderResponseDto(order.getId(), order.getUser().getId(), order.getOrderStatus(),
			order.getItems().stream()
				.map(i -> new OrderItemDto(i.getId(), i.getProduct().getName(), i.getProduct().getRealPrice(),
					i.getQuantity()))
				.collect(Collectors.toList()),
			order.getTotalPrice());
	}
}
