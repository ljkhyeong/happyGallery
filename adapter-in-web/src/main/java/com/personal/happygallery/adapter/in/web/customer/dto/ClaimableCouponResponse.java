package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ClaimableCouponResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long definitionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CouponDiscountType discountType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long discountValue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long minOrderAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long maxDiscountAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime validFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime validUntil
) {
    public static ClaimableCouponResponse from(CouponDefinition definition) {
        return new ClaimableCouponResponse(
                definition.getId(),
                definition.getName(),
                definition.getDiscountType(),
                definition.getDiscountValue(),
                definition.getMinOrderAmount(),
                definition.getMaxDiscountAmount(),
                definition.getValidFrom(),
                definition.getValidUntil());
    }
}
