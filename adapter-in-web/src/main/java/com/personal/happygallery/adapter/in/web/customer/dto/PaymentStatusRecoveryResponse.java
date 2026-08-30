package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase.CustomerPaymentStatus;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase.RecoveredPayment;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase.RecoveryResult;
import com.personal.happygallery.domain.payment.PaymentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record PaymentStatusRecoveryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String statusToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PaymentSummary> payments
) {
    public static PaymentStatusRecoveryResponse from(RecoveryResult result) {
        return new PaymentStatusRecoveryResponse(
                result.statusToken(),
                result.expiresAt().atOffset(ZoneOffset.UTC),
                result.payments().stream().map(PaymentSummary::from).toList());
    }

    public record PaymentSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orderId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentContext context,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CustomerPaymentStatus status
    ) {
        private static PaymentSummary from(RecoveredPayment payment) {
            return new PaymentSummary(
                    payment.orderId(), payment.context(), payment.amount(), payment.status());
        }
    }
}
