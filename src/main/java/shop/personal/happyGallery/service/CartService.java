package shop.personal.happyGallery.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.CartItemRequestDto;
import shop.personal.happyGallery.dto.CartResponseDto;
import shop.personal.happyGallery.model.Cart;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.repository.CartRepository;
import shop.personal.happyGallery.repository.ProductRepository;
import shop.personal.happyGallery.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CartService {

	private final UserRepository userRepository;
	private final CartRepository cartRepository;
	private final ProductRepository productRepository;

	@Transactional(readOnly = true)
	public CartResponseDto getCart(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저 없음"));

		return CartResponseDto.from(user.getCart());
	}

	@Transactional
	public CartResponseDto addItem(CartItemRequestDto requestDto) {
		User user = userRepository.findById(requestDto.getUserId())
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저 없음"));

		Cart cart = user.getCart();

		Product product = productRepository.findById(requestDto.getProductId())
			.orElseThrow(() -> new IllegalArgumentException("해당하는 상품 없음"));

		cart.addItem(product, requestDto.getQuantity());

		return CartResponseDto.from(cart);
	}

	@Transactional
	public CartResponseDto changeQuantity(CartItemRequestDto requestDto) {
		User user = userRepository.findById(requestDto.getUserId())
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저 없음"));

		Cart cart = user.getCart();

		Product product = productRepository.findById(requestDto.getProductId())
			.orElseThrow(() -> new IllegalArgumentException("해당하는 상품 없음"));

		cart.changeQuantity(product, requestDto.getQuantity());

		return CartResponseDto.from(cart);
	}

	@Transactional
	public void clearCart(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저 없음"));

		Cart cart = user.getCart();
		cart.clear();
	}

}
