package shop.personal.happyGallery.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.ProductRequestDto;
import shop.personal.happyGallery.dto.ProductResponseDto;
import shop.personal.happyGallery.model.Category;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.repository.CategoryRepository;
import shop.personal.happyGallery.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	@Transactional(readOnly = true)
	public ProductResponseDto getProductInfo(Long productId) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 상품 없음"));
		return ProductResponseDto.from(product);
	}

	@Transactional(readOnly = true)
	public List<ProductResponseDto> getCategoryProducts(Long categoryId) {
		Category category = categoryRepository.findById(categoryId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 카테고리 없음"));

		List<Product> products = productRepository.findByCategoryId(categoryId);

		return products.stream()
			.map(ProductResponseDto::from)
			.collect(Collectors.toList());
	}

	@Transactional
	public void registerProduct(Product product) {
		productRepository.save(product);
	}

	@Transactional
	public void removeProduct(Long productId) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 상품 없음"));
		productRepository.delete(product);
	}


}
