package shop.personal.happyGallery.model;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.personal.happyGallery.model.embeded.BaseTimeEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Product extends BaseTimeEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;
	private int price;
	private int realPrice;
	private int stock;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	public void increaseStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("수량은 0개 이상 입력해주세요.");
		}
		if (quantity > 1000)
			throw new IllegalArgumentException("추가할 수량은 1000개 이하로 입력해주세요.");

		stock += quantity;
	}

	public void decreaseStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("수량은 0개 이상 입력해주세요.");
		}
		if (!isInStock(quantity)) {
			throw new IllegalArgumentException("감소할 수량이 재고보다 많습니다.");
		}

		stock -= quantity;
	}

	public boolean isInStock(int quantity) {
		return stock >= quantity;
	}

	public void addCategory(Category category) {
		this.category = category;
		category.getProducts().add(this);
	}
}
