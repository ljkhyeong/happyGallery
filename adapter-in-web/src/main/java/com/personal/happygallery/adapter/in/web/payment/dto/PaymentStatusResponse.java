package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase.CustomerPaymentStatus;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase.PaymentStatusResult;
import com.personal.happygallery.domain.payment.PaymentContext;
import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentStatusResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentContext context,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CustomerPaymentStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long domainId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String accessToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean accessRecoveryRequired
) {

    public static PaymentStatusResponse from(PaymentStatusResult result) {
        return new PaymentStatusResponse(
                result.context(), result.amount(), result.status(), result.domainId(), result.accessToken(),
                result.accessRecoveryRequired());
    }
}
