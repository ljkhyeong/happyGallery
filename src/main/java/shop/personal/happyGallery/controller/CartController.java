package shop.personal.happyGallery.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.CartItemAddRequestDto;
import shop.personal.happyGallery.dto.CartItemChangeRequestDto;
import shop.personal.happyGallery.dto.CartResponseDto;
import shop.personal.happyGallery.service.CartService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {

	private final CartService cartService;

	@GetMapping
	public ResponseEntity<CartResponseDto> getCart(@RequestParam Long userId) {
		return ResponseEntity.ok()
			.body(cartService.getCart(userId));
	}

	@PostMapping("/items")
	public ResponseEntity<CartResponseDto> addItem(@RequestBody CartItemAddRequestDto requestDto) {
		return ResponseEntity.ok()
			.body(cartService.addItem(requestDto.userId(), requestDto.userId(), requestDto.quantity()));
	}
	@PutMapping("/items")
	public ResponseEntity<CartResponseDto> changeQuantity(@RequestBody CartItemChangeRequestDto requestDto) {
		return ResponseEntity.ok()
			.body(cartService.changeQuantity(requestDto.userId(), requestDto.productId(), requestDto.quantity()));
	}



}
