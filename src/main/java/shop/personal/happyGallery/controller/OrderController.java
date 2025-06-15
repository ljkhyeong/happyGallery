package shop.personal.happyGallery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.OrderResponseDto;
import shop.personal.happyGallery.service.OrderService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;

	@GetMapping("/{id}")
	public OrderResponseDto getOrderInfo(@PathVariable Long id) {
		return orderService.getOrderInfo(id);
	}

	@PostMapping
	public OrderResponseDto createOrderFromCart(@RequestParam Long userId) {
		return orderService.createOrderFromCart(userId);
	}
}
