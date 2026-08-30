package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.adapter.in.web.policy.dto.PolicyAcceptanceRequest;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "BookingPayload")
public record BookingPaymentPayloadRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "BOOKING")
        String type,
        @Schema(nullable = true) Long userId,
        @Schema(nullable = true) String phone,
        @Schema(nullable = true) String verificationCode,
        @Schema(nullable = true) String name,
        @NotNull Long slotId,
        @Schema(nullable = true) Long passId,
        @Schema(nullable = true, allowableValues = {"CARD", "EASY_PAY"})
        DepositPaymentMethod paymentMethod,
        @Min(1)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        int participantCount,
        @Valid @Schema(nullable = true) PolicyAcceptanceRequest policyAcceptance
) implements PaymentPayloadRequest {

    @Override
    public PaymentPayload.BookingPayload toCommand() {
        return new PaymentPayload.BookingPayload(
                userId,
                phone,
                verificationCode,
                name,
                slotId,
                passId,
                paymentMethod,
                participantCount,
                policyAcceptance == null ? null : policyAcceptance.toCommand());
    }
}
