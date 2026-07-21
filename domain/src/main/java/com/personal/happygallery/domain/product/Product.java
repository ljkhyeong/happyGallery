package com.personal.happygallery.domain.product;

import com.personal.happygallery.domain.category.CategoryName;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.URI;
import java.time.LocalDateTime;

/** 판매 상품 — products 테이블 */
@Entity
@Table(name = "products")
public class Product {

    public static final long MAX_PRICE = PaymentAmountPolicy.MAX_AMOUNT;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 5_000;
    public static final int MAX_IMAGE_URL_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private long price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = MAX_IMAGE_URL_LENGTH)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Product() {}

    /**
     * 상품 생성. 초기 상태는 {@link ProductStatus#ACTIVE}.
     *
     * @param name  상품명
     * @param type  상품 유형 (READY_STOCK | MADE_TO_ORDER)
     * @param price 가격 (원)
     */
    public Product(String name, ProductType type, long price) {
        this(name, type, null, price);
    }

    /**
     * 카테고리를 포함한 상품 생성.
     *
     * @param name     상품명
     * @param type     상품 유형
     * @param category 카테고리 (nullable)
     * @param price    가격 (원)
     */
    public Product(String name, ProductType type, String category, long price) {
        this(name, type, category, price, null, null);
    }

    public Product(String name, ProductType type, String category, long price,
                   String description, String imageUrl) {
        this.type = type;
        updateDetails(name, category, price, description, imageUrl);
        this.status = ProductStatus.ACTIVE;
    }

    public void updateDetails(String name, String category, long price,
                              String description, String imageUrl) {
        if (price < 1L || price > MAX_PRICE) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "상품 가격은 1원 이상 허용 범위 이하여야 합니다.");
        }
        this.name = requireText(name, "상품명", MAX_NAME_LENGTH);
        this.category = CategoryName.optional(category);
        this.price = price;
        this.description = optionalText(description, "상품 설명", MAX_DESCRIPTION_LENGTH);
        this.imageUrl = optionalImageUrl(imageUrl);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        String normalized = optionalText(value, fieldName, maxLength);
        if (normalized == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, fieldName + "은 필수입니다.");
        }
        return normalized;
    }

    private static String optionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, fieldName + "은 " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    private static String optionalImageUrl(String value) {
        String normalized = optionalText(value, "대표 이미지 URL", MAX_IMAGE_URL_LENGTH);
        if (normalized == null || normalized.startsWith("/")) {
            return normalized;
        }
        try {
            URI uri = URI.create(normalized);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) {
                return normalized;
            }
        } catch (IllegalArgumentException ignored) {
            // 아래의 일관된 도메인 오류로 변환한다.
        }
        throw new HappyGalleryException(
                ErrorCode.INVALID_INPUT, "대표 이미지 URL은 http(s) 주소 또는 /로 시작하는 경로여야 합니다.");
    }

    /** 상품을 비활성화한다. */
    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    /** 판매 중지 상품을 다시 활성화한다. */
    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ProductType getType() { return type; }
    public String getCategory() { return category; }
    public long getPrice() { return price; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public ProductStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
