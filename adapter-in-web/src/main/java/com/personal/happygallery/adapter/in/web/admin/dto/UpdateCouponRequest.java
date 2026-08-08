package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.coupon.port.in.CouponDefinitionCommand;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateCouponRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank @Size(max = CouponDefinition.MAX_NAME_LENGTH) String name,
        @NotNull CouponDiscountType discountType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive @Max(PaymentAmountPolicy.MAX_AMOUNT) long discountValue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @PositiveOrZero @Max(PaymentAmountPolicy.MAX_AMOUNT) long minOrderAmount,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        @Positive @Max(PaymentAmountPolicy.MAX_AMOUNT) Long maxDiscountAmount,
        @NotNull LocalDateTime validFrom,
        @NotNull LocalDateTime validUntil,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean active,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean publiclyClaimable
) {
    public CouponDefinitionCommand toCommand() {
        return new CouponDefinitionCommand(
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
}
