package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareResult;
import com.personal.happygallery.domain.payment.PaymentContext;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * prepare 응답.
 *
 * <p>{@code amount}가 0이면 프론트는 Toss 결제창을 거치지 않고 바로 confirm을 호출한다 (8회권 사용 예약 등).
 */
public record PreparePaymentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentContext context,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String statusToken
) {

    public static PreparePaymentResponse from(PrepareResult r) {
        return new PreparePaymentResponse(r.orderId(), r.amount(), r.context(), r.statusToken());
    }
}
