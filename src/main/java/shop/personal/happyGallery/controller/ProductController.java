package shop.personal.happyGallery.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.ProductRegisterRequestDto;
import shop.personal.happyGallery.dto.ProductResponseDto;
import shop.personal.happyGallery.service.ProductService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;

	@GetMapping("/{id}")
	public ProductResponseDto getProduct(@PathVariable Long id) {
		return productService.getProductInfo(id);
	}

	@GetMapping
	public List<ProductResponseDto> getCategoryProducts(@RequestParam Long categoryId) {
		return productService.getCategoryProducts(categoryId);
	}

	@PostMapping
	public void registerProduct(@RequestBody ProductRegisterRequestDto requestDto) {
		productService.registerProduct(requestDto.toEntity());
	}

	@DeleteMapping("/{id}")
	public void removeProduct(@PathVariable Long id) {
		productService.removeProduct(id);
	}
}
