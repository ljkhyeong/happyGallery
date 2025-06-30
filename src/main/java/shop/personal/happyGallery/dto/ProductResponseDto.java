package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.embeded.Money;


public record ProductResponseDto(
	Long id,
	String name,
	String description,
	Money price,
	Money realPrice,
	int stock
) {

	public static ProductResponseDto from(Product product) {
		return new ProductResponseDto(product.getId(), product.getName(), product.getDescription(),
			product.getPrice(), product.getRealPrice(), product.getStock());
	}
}
