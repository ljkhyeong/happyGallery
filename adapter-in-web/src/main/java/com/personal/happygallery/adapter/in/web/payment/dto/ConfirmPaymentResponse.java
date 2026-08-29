package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.domain.payment.PaymentContext;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * confirm 응답.
 *
 * <p>비회원 경로는 {@code accessToken}이 함께 반환되며, 프론트는 이후 X-Access-Token 헤더로 사용한다.
 */
public record ConfirmPaymentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentContext context,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long domainId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String accessToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean accessRecoveryRequired,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String receiptUrl
) {

    public static ConfirmPaymentResponse from(ConfirmResult r) {
        return new ConfirmPaymentResponse(
                r.context(), r.domainId(), r.accessToken(), r.accessRecoveryRequired(), r.receiptUrl());
    }
}
