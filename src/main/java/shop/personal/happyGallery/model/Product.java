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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.personal.happyGallery.exception.ApplicationException;
import shop.personal.happyGallery.exception.ErrorCode;
import shop.personal.happyGallery.model.embeded.BaseTimeEntity;
import shop.personal.happyGallery.model.embeded.Money;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Product extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private int version;

	private String name;
	private String description;
	private Money price;
	private Money realPrice;
	private int stock;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	public void increaseStock(int quantity) {
		verifyPositive(quantity);
		if (quantity > 1000)
			throw new ApplicationException(ErrorCode.INVALID_ARGUMENT);

		stock += quantity;
	}

	public void decreaseStock(int quantity) {
		verifyPositive(quantity);
		if (!isInStock(quantity)) {
			throw new ApplicationException(ErrorCode.INVALID_ARGUMENT);
		}

		stock -= quantity;
	}

	private void verifyPositive(int quantity) {
		if (quantity <= 0) {
			throw new ApplicationException(ErrorCode.INVALID_ARGUMENT);
		}
	}

	public boolean isInStock(int quantity) {
		return stock >= quantity;
	}

	public void addCategory(Category category) {
		this.category = category;
		category.getProducts().add(this);
	}

	private void verifyVersion() {

	}
}
