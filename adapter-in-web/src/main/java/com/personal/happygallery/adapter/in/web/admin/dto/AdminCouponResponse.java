package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminCouponResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CouponDiscountType discountType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long discountValue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long minOrderAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long maxDiscountAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime validFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime validUntil,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean active,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean publiclyClaimable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version
) {
    public static AdminCouponResponse from(CouponDefinition definition) {
        return new AdminCouponResponse(
                definition.getId(),
                definition.getName(),
                definition.getDiscountType(),
                definition.getDiscountValue(),
                definition.getMinOrderAmount(),
                definition.getMaxDiscountAmount(),
                definition.getValidFrom(),
                definition.getValidUntil(),
                definition.isActive(),
                definition.isPubliclyClaimable(),
                definition.getVersion());
    }
}
