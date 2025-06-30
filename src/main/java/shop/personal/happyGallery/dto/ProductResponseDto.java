package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.Product;
import shop.personal.happyGallery.model.embeded.Money;

@AllArgsConstructor
@Getter
public class ProductResponseDto {
	private final Long id;
	private final String name;
	private final String description;
	private final Money price;
	private final Money realPrice;
	private final int stock;

	public static ProductResponseDto from(Product product) {
		return new ProductResponseDto(product.getId(), product.getName(), product.getDescription(),
			product.getPrice(), product.getRealPrice(), product.getStock());
	}
}
