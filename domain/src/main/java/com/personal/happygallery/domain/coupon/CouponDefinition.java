package com.personal.happygallery.domain.coupon;

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
import java.time.LocalDateTime;

/** 관리자 쿠폰 정의 — coupon_definitions 테이블. */
@Entity
@Table(name = "coupon_definitions")
public class CouponDefinition {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MIN_PERCENT = 1;
    public static final int MAX_PERCENT = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private CouponDiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private long discountValue;

    @Column(name = "min_order_amount", nullable = false)
    private long minOrderAmount;

    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "publicly_claimable", nullable = false)
    private boolean publiclyClaimable;

    @Version
    @Column(nullable = false)
    private long version;

    protected CouponDefinition() {}

    public CouponDefinition(String name,
                            CouponDiscountType discountType,
                            long discountValue,
                            long minOrderAmount,
                            Long maxDiscountAmount,
                            LocalDateTime validFrom,
                            LocalDateTime validUntil,
                            boolean active,
                            boolean publiclyClaimable) {
        applyDefinition(
                name,
                discountType,
                discountValue,
                minOrderAmount,
                maxDiscountAmount,
                validFrom,
                validUntil,
                active,
                publiclyClaimable);
    }

    public void update(String name,
                       CouponDiscountType discountType,
                       long discountValue,
                       long minOrderAmount,
                       Long maxDiscountAmount,
                       LocalDateTime validFrom,
                       LocalDateTime validUntil,
                       boolean active,
                       boolean publiclyClaimable) {
        applyDefinition(
                name,
                discountType,
                discountValue,
                minOrderAmount,
                maxDiscountAmount,
                validFrom,
                validUntil,
                active,
                publiclyClaimable);
    }

    private void applyDefinition(String name,
                                 CouponDiscountType discountType,
                                 long discountValue,
                                 long minOrderAmount,
                                 Long maxDiscountAmount,
                                 LocalDateTime validFrom,
                                 LocalDateTime validUntil,
                                 boolean active,
                                 boolean publiclyClaimable) {
        this.name = requireName(name);
        this.discountType = requireDiscountType(discountType);
        requireAmountRange(minOrderAmount, "최소 주문 금액");
        validateDiscount(discountType, discountValue, maxDiscountAmount);
        if (validFrom == null || validUntil == null || !validFrom.isBefore(validUntil)) {
            throw invalid("쿠폰 종료 시각은 시작 시각보다 뒤여야 합니다.");
        }
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.active = active;
        this.publiclyClaimable = publiclyClaimable;
    }

    /** 공개 발급 시점에 활성·공개·유효기간 조건을 모두 검증한다. */
    public void requirePubliclyClaimableAt(LocalDateTime now) {
        requireTime(now);
        if (!active) {
            throw unavailable("현재 발급할 수 없는 쿠폰입니다.");
        }
        if (!publiclyClaimable) {
            throw unavailable("공개 발급 대상이 아닌 쿠폰입니다.");
        }
        if (!isWithinValidity(now)) {
            throw unavailable("쿠폰 발급 기간이 아닙니다.");
        }
    }

    public boolean isPubliclyClaimableAt(LocalDateTime now) {
        requireTime(now);
        return active && publiclyClaimable && isWithinValidity(now);
    }

    /** 배송비를 제외한 주문 상품 금액만으로 할인액을 계산한다. */
    public long calculateDiscount(long productAmount, LocalDateTime now) {
        PaymentAmountPolicy.requireValid(productAmount);
        requireTime(now);
        if (!active || !isWithinValidity(now)) {
            throw unavailable("사용할 수 없는 쿠폰입니다.");
        }
        if (productAmount < minOrderAmount) {
            throw unavailable("쿠폰 최소 주문 금액을 충족하지 못했습니다.");
        }

        long calculated = switch (discountType) {
            case FIXED -> discountValue;
            case PERCENT -> Math.min(
                    Math.multiplyExact(productAmount, discountValue) / 100L,
                    maxDiscountAmount);
        };
        return Math.min(productAmount, calculated);
    }

    public boolean isWithinValidity(LocalDateTime now) {
        requireTime(now);
        return !now.isBefore(validFrom) && now.isBefore(validUntil);
    }

    /** 삭제 API는 발급 이력을 보존하기 위해 정의를 비활성화한다. */
    public void deactivate() {
        this.active = false;
        this.publiclyClaimable = false;
    }

    private static void validateDiscount(CouponDiscountType type,
                                         long discountValue,
                                         Long maxDiscountAmount) {
        if (type == CouponDiscountType.FIXED) {
            requirePositiveAmount(discountValue, "정액 할인 금액");
            if (maxDiscountAmount != null) {
                throw invalid("정액 할인 쿠폰에는 최대 할인 금액을 설정할 수 없습니다.");
            }
            return;
        }
        if (discountValue < MIN_PERCENT || discountValue > MAX_PERCENT) {
            throw invalid("정률 할인율은 1 이상 100 이하의 정수여야 합니다.");
        }
        if (maxDiscountAmount == null) {
            throw invalid("정률 할인 쿠폰의 최대 할인 금액은 필수입니다.");
        }
        requirePositiveAmount(maxDiscountAmount, "최대 할인 금액");
    }

    private static CouponDiscountType requireDiscountType(CouponDiscountType discountType) {
        if (discountType == null) {
            throw invalid("쿠폰 할인 방식은 필수입니다.");
        }
        return discountType;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw invalid("쿠폰 이름은 필수입니다.");
        }
        String normalized = name.strip();
        if (normalized.codePointCount(0, normalized.length()) > MAX_NAME_LENGTH) {
            throw invalid("쿠폰 이름은 " + MAX_NAME_LENGTH + "자 이하여야 합니다.");
        }
        return normalized;
    }

    private static void requirePositiveAmount(long amount, String fieldName) {
        if (amount < 1L || amount > PaymentAmountPolicy.MAX_AMOUNT) {
            throw invalid(fieldName + "은 1원 이상 허용 범위 이하여야 합니다.");
        }
    }

    private static void requireAmountRange(long amount, String fieldName) {
        if (amount < 0L || amount > PaymentAmountPolicy.MAX_AMOUNT) {
            throw invalid(fieldName + "이 허용 범위를 초과했습니다.");
        }
    }

    private static void requireTime(LocalDateTime now) {
        if (now == null) {
            throw invalid("쿠폰 기준 시각이 누락되었습니다.");
        }
    }

    private static HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }

    private static HappyGalleryException unavailable(String message) {
        return new HappyGalleryException(ErrorCode.CHANGE_NOT_ALLOWED, message);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CouponDiscountType getDiscountType() { return discountType; }
    public long getDiscountValue() { return discountValue; }
    public long getMinOrderAmount() { return minOrderAmount; }
    public Long getMaxDiscountAmount() { return maxDiscountAmount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public boolean isActive() { return active; }
    public boolean isPubliclyClaimable() { return publiclyClaimable; }
    public long getVersion() { return version; }
}
