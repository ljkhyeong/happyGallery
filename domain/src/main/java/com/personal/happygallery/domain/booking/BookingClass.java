package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.category.CategoryName;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.media.ImageReferencePolicy;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 체험 클래스 — classes 테이블 */
@Entity
@Table(name = "classes")
public class BookingClass {

    /** 10% 예약금이 최소 1원이 되는 클래스 가격 하한. */
    public static final long MIN_PRICE = 10L;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 5_000;
    public static final int MAX_IMAGE_URL_LENGTH = ImageReferencePolicy.MAX_LENGTH;
    public static final int MAX_PREPARATION_INFO_LENGTH = 2_000;
    public static final int MAX_TARGET_AUDIENCE_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** PERFUME | WOOD | KNIT | POP | ... */
    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    /** 원(KRW) 단위 */
    @Column(nullable = false)
    private long price;

    /** 뒤쪽 버퍼(분), 기본 30분 */
    @Column(name = "buffer_min", nullable = false)
    private int bufferMin = 30;

    /** 자동 생성되는 회차의 최대 예약 인원 */
    @Column(nullable = false)
    private int capacity = SlotCapacity.DEFAULT;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = MAX_IMAGE_URL_LENGTH)
    private String imageUrl;

    @Column(name = "preparation_info", length = MAX_PREPARATION_INFO_LENGTH)
    private String preparationInfo;

    @Column(name = "target_audience", length = MAX_TARGET_AUDIENCE_LENGTH)
    private String targetAudience;

    @Column(name = "pass_eligible", nullable = false)
    private boolean passEligible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingClassStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BookingClass() {}

    public BookingClass(String name, String category, int durationMin, long price, int bufferMin) {
        this(name, category, durationMin, price, bufferMin, SlotCapacity.DEFAULT,
                !"PERFUME".equals(CategoryName.required(category)),
                null, null, null, null);
    }

    public BookingClass(String name,
                        String category,
                        int durationMin,
                        long price,
                        int bufferMin,
                        boolean passEligible,
                        String description,
                        String imageUrl,
                        String preparationInfo,
                        String targetAudience) {
        this(name, category, durationMin, price, bufferMin, SlotCapacity.DEFAULT,
                passEligible, description, imageUrl, preparationInfo, targetAudience);
    }

    public BookingClass(String name,
                        String category,
                        int durationMin,
                        long price,
                        int bufferMin,
                        int capacity,
                        boolean passEligible,
                        String description,
                        String imageUrl,
                        String preparationInfo,
                        String targetAudience) {
        if (durationMin < 1) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "클래스 소요 시간은 1분 이상이어야 합니다.");
        }
        if (bufferMin < 0) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "클래스 버퍼는 0분 이상이어야 합니다.");
        }
        SlotCapacity.requireValidCapacity(capacity);
        this.durationMin = durationMin;
        this.bufferMin = bufferMin;
        this.capacity = capacity;
        applyDetails(
                name, category, price, passEligible,
                description, imageUrl, preparationInfo, targetAudience);
        this.status = BookingClassStatus.ACTIVE;
    }

    public void updateDetails(String name,
                              String category,
                              long price,
                              boolean passEligible,
                              String description,
                              String imageUrl,
                              String preparationInfo,
                              String targetAudience) {
        applyDetails(
                name, category, price, passEligible,
                description, imageUrl, preparationInfo, targetAudience);
    }

    private void applyDetails(String name,
                              String category,
                              long price,
                              boolean passEligible,
                              String description,
                              String imageUrl,
                              String preparationInfo,
                              String targetAudience) {
        if (price < MIN_PRICE || price > PaymentAmountPolicy.MAX_AMOUNT) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "클래스 가격은 %d원 이상 허용 범위 이하여야 합니다.".formatted(MIN_PRICE));
        }
        this.name = requireName(name);
        this.category = CategoryName.required(category);
        this.price = price;
        this.passEligible = passEligible;
        this.description = optionalText(description, "클래스 설명", MAX_DESCRIPTION_LENGTH);
        this.imageUrl = ImageReferencePolicy.optional(imageUrl);
        this.preparationInfo = optionalText(
                preparationInfo, "준비물 안내", MAX_PREPARATION_INFO_LENGTH);
        this.targetAudience = optionalText(
                targetAudience, "대상 안내", MAX_TARGET_AUDIENCE_LENGTH);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "클래스명은 필수입니다.");
        }
        String normalized = name.strip();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "클래스명은 100자 이하여야 합니다.");
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

    public void changeStatus(BookingClassStatus status) {
        this.status = status;
    }

    public void requireActive() {
        if (!isActive()) {
            throw new HappyGalleryException(ErrorCode.CLASS_INACTIVE);
        }
    }

    public boolean isActive() {
        return status == BookingClassStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getDurationMin() { return durationMin; }
    public long getPrice() { return price; }
    public int getBufferMin() { return bufferMin; }
    public int getCapacity() { return capacity; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getPreparationInfo() { return preparationInfo; }
    public String getTargetAudience() { return targetAudience; }
    public boolean isPassEligible() { return passEligible; }
    public BookingClassStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
