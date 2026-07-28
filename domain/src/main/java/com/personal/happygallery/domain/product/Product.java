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
import jakarta.persistence.Version;
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
    public static final int MAX_SPECIFICATION_LENGTH = 2_000;
    public static final int MAX_CARE_INSTRUCTIONS_LENGTH = 2_000;
    public static final int MIN_PRODUCTION_LEAD_DAYS = 1;
    public static final int MAX_PRODUCTION_LEAD_DAYS = 180;

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

    @Column(length = MAX_SPECIFICATION_LENGTH)
    private String specification;

    @Column(name = "care_instructions", length = MAX_CARE_INSTRUCTIONS_LENGTH)
    private String careInstructions;

    @Column(name = "production_lead_days")
    private Integer productionLeadDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProductStatus status;

    @Version
    @Column(nullable = false)
    private long version;

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
        this(name, type, category, price, description, imageUrl, null, null, null);
    }

    public Product(String name, ProductType type, String category, long price,
                   String description, String imageUrl, String specification,
                   String careInstructions, Integer productionLeadDays) {
        if (type == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "상품 유형은 필수입니다.");
        }
        this.type = type;
        applyDetails(
                name, category, price, description, imageUrl,
                specification, careInstructions, productionLeadDays);
        this.status = ProductStatus.ACTIVE;
    }

    public void updateDetails(String name, String category, long price,
                              String description, String imageUrl) {
        applyDetails(
                name, category, price, description, imageUrl,
                specification, careInstructions, productionLeadDays);
    }

    public void updateDetails(String name, String category, long price,
                              String description, String imageUrl, String specification,
                              String careInstructions, Integer productionLeadDays) {
        applyDetails(
                name, category, price, description, imageUrl,
                specification, careInstructions, productionLeadDays);
    }

    private void applyDetails(String name, String category, long price,
                              String description, String imageUrl, String specification,
                              String careInstructions, Integer productionLeadDays) {
        if (price < 1L || price > MAX_PRICE) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "상품 가격은 1원 이상 허용 범위 이하여야 합니다.");
        }
        this.name = requireText(name, "상품명", MAX_NAME_LENGTH);
        this.category = CategoryName.optional(category);
        this.price = price;
        this.description = optionalText(description, "상품 설명", MAX_DESCRIPTION_LENGTH);
        this.imageUrl = optionalImageUrl(imageUrl);
        this.specification = optionalText(
                specification, "상품 사양", MAX_SPECIFICATION_LENGTH);
        this.careInstructions = optionalText(
                careInstructions, "관리 방법", MAX_CARE_INSTRUCTIONS_LENGTH);
        this.productionLeadDays = productionLeadDays;
        requireTypeSpecificPurchaseTerms();
    }

    private void requireTypeSpecificPurchaseTerms() {
        if (type == ProductType.MADE_TO_ORDER) {
            if (specification == null) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "주문제작 상품 사양은 필수입니다.");
            }
            if (productionLeadDays == null
                    || productionLeadDays < MIN_PRODUCTION_LEAD_DAYS
                    || productionLeadDays > MAX_PRODUCTION_LEAD_DAYS) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT,
                        "주문제작 상품 제작 기간은 "
                                + MIN_PRODUCTION_LEAD_DAYS + "일 이상 "
                                + MAX_PRODUCTION_LEAD_DAYS + "일 이하여야 합니다.");
            }
            return;
        }
        if (productionLeadDays != null) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "기성품에는 제작 기간을 설정할 수 없습니다.");
        }
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
    public String getSpecification() { return specification; }
    public String getCareInstructions() { return careInstructions; }
    public Integer getProductionLeadDays() { return productionLeadDays; }
    public ProductStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
