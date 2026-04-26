package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareResult;
import com.personal.happygallery.domain.payment.PaymentContext;

/**
 * prepare 응답.
 *
 * <p>{@code amount}가 0이면 프론트는 Toss 결제창을 거치지 않고 바로 confirm을 호출한다 (8회권 사용 예약 등).
 */
public record PreparePaymentResponse(String orderId, long amount, PaymentContext context) {

    public static PreparePaymentResponse from(PrepareResult r) {
        return new PreparePaymentResponse(r.orderId(), r.amount(), r.context());
    }
}
