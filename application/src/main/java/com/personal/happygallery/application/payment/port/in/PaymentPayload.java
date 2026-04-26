package com.personal.happygallery.application.payment.port.in;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import java.util.List;

/**
 * prepare 단계에 들어오는 context별 결제 payload.
 *
 * <p>{@link com.fasterxml.jackson.annotation.JsonTypeInfo} 기반 polymorphic 직렬화로
 * {@link com.personal.happygallery.domain.payment.PaymentAttempt#getPayloadJson()}에 저장되고,
 * confirm 시 fulfiller가 동일 클래스로 역직렬화해 도메인 저장에 사용한다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentPayload.OrderPayload.class, name = "ORDER"),
        @JsonSubTypes.Type(value = PaymentPayload.BookingPayload.class, name = "BOOKING"),
        @JsonSubTypes.Type(value = PaymentPayload.PassPayload.class, name = "PASS")
})
public sealed interface PaymentPayload {

    /**
     * 주문 결제 payload.
     *
     * <p>{@code phone/code/name}이 채워지면 비회원 휴대폰 인증 경로,
     * {@code userId}가 채워지면 회원 경로다.
     */
    record OrderPayload(
            Long userId,
            String phone,
            String verificationCode,
            String name,
            List<OrderItemRef> items
    ) implements PaymentPayload {}

    record OrderItemRef(Long productId, int qty) {}

    /**
     * 예약 결제 payload. 회원 8회권 사용 시 {@code passId}만 세팅(amount=0).
     */
    record BookingPayload(
            Long userId,
            String phone,
            String verificationCode,
            String name,
            Long slotId,
            Long passId,
            DepositPaymentMethod paymentMethod
    ) implements PaymentPayload {}

    /** 8회권 구매 payload. 회원 전용 — userId 필수. */
    record PassPayload(Long userId) implements PaymentPayload {}
}
