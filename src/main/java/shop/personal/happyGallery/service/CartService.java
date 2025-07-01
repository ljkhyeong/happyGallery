package shop.personal.happyGallery.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.CartResponseDto;
import shop.personal.happyGallery.model.Cart;
import shop.personal.happyGallery.model.Product;
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
		Cart cart = cartRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저의 카트 없음"));

		return CartResponseDto.from(cart);
	}

	@Transactional
	public CartResponseDto addItem(Long userId, Long ProductId, int quantity) {
		Cart cart = cartRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저의 카트 없음"));

		Product product = productRepository.findById(ProductId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 상품 없음"));

		cart.addItem(product, quantity);

		return CartResponseDto.from(cart);
	}

	@Transactional
	public CartResponseDto changeQuantity(Long userId, Long productId, int quantity) {
		Cart cart = cartRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저의 카트 없음"));

		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 상품 없음"));

		cart.changeQuantity(product, quantity);

		return CartResponseDto.from(cart);
	}

	@Transactional
	public void clearCart(Long userId) {
		Cart cart = cartRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 유저의 카트 없음"));

		cart.clear();
	}

}
