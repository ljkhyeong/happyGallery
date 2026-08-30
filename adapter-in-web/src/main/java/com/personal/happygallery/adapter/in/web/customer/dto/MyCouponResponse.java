package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.coupon.port.in.CouponMemberUseCase.IssuedCouponView;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MyCouponResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long definitionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CouponDiscountType discountType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long discountValue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long minOrderAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long maxDiscountAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime validFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime validUntil,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IssuedCouponStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime claimedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime reservedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime usedAt
) {
    public static MyCouponResponse from(IssuedCouponView view) {
        IssuedCoupon issued = view.issuedCoupon();
        CouponDefinition definition = view.definition();
        return new MyCouponResponse(
                issued.getId(),
                definition.getId(),
                definition.getName(),
                definition.getDiscountType(),
                definition.getDiscountValue(),
                definition.getMinOrderAmount(),
                definition.getMaxDiscountAmount(),
                definition.getValidFrom(),
                definition.getValidUntil(),
                issued.getStatus(),
                issued.getClaimedAt(),
                issued.getReservedAt(),
                issued.getUsedAt());
    }
}
