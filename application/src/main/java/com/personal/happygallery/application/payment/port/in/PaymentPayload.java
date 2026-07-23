package com.personal.happygallery.application.payment.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.List;

/**
 * 공개 prepare 요청에서 받는 context별 결제 payload.
 *
 * <p>prepare가 만든 서버 확정 스냅샷은
 * {@link com.personal.happygallery.application.payment.context.PreparedPaymentPayload}로 분리해
 * 공개 요청 타입과 암호화 저장 타입이 섞이지 않게 한다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentPayload.OrderPayload.class, name = "ORDER"),
        @JsonSubTypes.Type(value = PaymentPayload.BookingPayload.class, name = "BOOKING"),
        @JsonSubTypes.Type(value = PaymentPayload.PassPayload.class, name = "PASS")
})
public sealed interface PaymentPayload {

    /** prepare 당시 결제 주체. 비회원이면 null이다. */
    Long userId();

    /**
     * 주문 결제 payload.
     *
     * <p>{@code phone/code/name}이 채워지면 비회원 휴대폰 인증 경로,
     * {@code userId}가 채워지면 회원 경로다. {@code cartCheckout=true}이면
     * 클라이언트의 {@code items} 대신 서버 장바구니의 구매 가능한 항목을 사용한다.
     */
    record OrderPayload(
            Long userId,
            String phone,
            String verificationCode,
            String name,
            List<OrderItemRef> items,
            @JsonProperty(required = true) boolean cartCheckout,
            FulfillmentType fulfillmentType,
            ShippingAddress shippingAddress,
            String madeToOrderConsentVersion,
            @JsonProperty(required = true) boolean madeToOrderConsent
    ) implements PaymentPayload {

        public OrderPayload {
            requireFulfillment(fulfillmentType, shippingAddress);
        }

        public OrderPayload(Long userId,
                            String phone,
                            String verificationCode,
                            String name,
                            List<OrderItemRef> items) {
            this(userId, phone, verificationCode, name, items, false, FulfillmentType.PICKUP, null);
        }

        public OrderPayload(Long userId,
                            String phone,
                            String verificationCode,
                            String name,
                            List<OrderItemRef> items,
                            boolean cartCheckout) {
            this(userId, phone, verificationCode, name, items, cartCheckout, FulfillmentType.PICKUP, null);
        }

        public OrderPayload(Long userId,
                            String phone,
                            String verificationCode,
                            String name,
                            List<OrderItemRef> items,
                            boolean cartCheckout,
                            FulfillmentType fulfillmentType,
                            ShippingAddress shippingAddress) {
            this(userId, phone, verificationCode, name, items, cartCheckout,
                    fulfillmentType, shippingAddress, null, false);
        }
    }

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

    private static void requireFulfillment(
            FulfillmentType fulfillmentType, ShippingAddress shippingAddress) {
        if (fulfillmentType == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "수령 방법을 선택해 주세요.");
        }
        if (fulfillmentType == FulfillmentType.SHIPPING && shippingAddress == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "배송지는 필수입니다.");
        }
        if (fulfillmentType == FulfillmentType.PICKUP && shippingAddress != null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "픽업 주문에는 배송지를 입력할 수 없습니다.");
        }
    }
}
