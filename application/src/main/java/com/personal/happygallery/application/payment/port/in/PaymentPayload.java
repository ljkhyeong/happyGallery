package com.personal.happygallery.application.payment.port.in;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.List;

/**
 * prepare 입력과 confirm에서 사용할 서버 확정 스냅샷을 나타내는 context별 결제 payload.
 *
 * <p>{@link com.fasterxml.jackson.annotation.JsonTypeInfo} 기반 polymorphic 직렬화로
 * 암호화되어 {@link com.personal.happygallery.domain.payment.PaymentAttempt#getPayloadEnc()}에 저장되고,
 * 공개 입력 record는 preparer까지만 사용하고, preparer가 만든 {@code Prepared*Payload}를 암호화해 저장한다.
 * confirm 시 fulfiller가 서버 확정 record로 역직렬화해 도메인 저장에 사용한다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentPayload.OrderPayload.class, name = "ORDER"),
        @JsonSubTypes.Type(value = PaymentPayload.PreparedOrderPayload.class, name = "PREPARED_ORDER"),
        @JsonSubTypes.Type(value = PaymentPayload.BookingPayload.class, name = "BOOKING"),
        @JsonSubTypes.Type(value = PaymentPayload.PreparedBookingPayload.class, name = "PREPARED_BOOKING"),
        @JsonSubTypes.Type(value = PaymentPayload.PassPayload.class, name = "PASS"),
        @JsonSubTypes.Type(value = PaymentPayload.PreparedPassPayload.class, name = "PREPARED_PASS")
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
            boolean cartCheckout,
            FulfillmentType fulfillmentType,
            ShippingAddress shippingAddress,
            String madeToOrderConsentVersion,
            boolean madeToOrderConsent
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

    /** prepare에서 서버 상품가와 비회원 결제 인증 증거를 확정한 뒤 암호화 저장하는 주문 payload. */
    record PreparedOrderPayload(
            Long userId,
            String phone,
            String guestVerificationProof,
            String name,
            List<PreparedOrderItem> items,
            boolean cartCheckout,
            FulfillmentType fulfillmentType,
            ShippingAddress shippingAddress,
            long shippingFee,
            MadeToOrderConsent madeToOrderConsent
    ) implements PaymentPayload {

        public PreparedOrderPayload {
            requireFulfillment(fulfillmentType, shippingAddress);
            if (shippingFee < 0L || (fulfillmentType == FulfillmentType.PICKUP && shippingFee != 0L)) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "수령 방법과 배송비가 일치하지 않습니다.");
            }
        }

        public PreparedOrderPayload(Long userId,
                                    String phone,
                                    String guestVerificationProof,
                                    String name,
                                    List<PreparedOrderItem> items) {
            this(userId, phone, guestVerificationProof, name, items,
                    false, FulfillmentType.PICKUP, null, 0L);
        }

        public PreparedOrderPayload(Long userId,
                                    String phone,
                                    String guestVerificationProof,
                                    String name,
                                    List<PreparedOrderItem> items,
                                    boolean cartCheckout,
                                    FulfillmentType fulfillmentType,
                                    ShippingAddress shippingAddress,
                                    long shippingFee) {
            this(userId, phone, guestVerificationProof, name, items, cartCheckout,
                    fulfillmentType, shippingAddress, shippingFee, null);
        }
    }

    record PreparedOrderItem(
            Long cartItemId,
            Long productId,
            String productName,
            int qty,
            long unitPrice
    ) {

        public PreparedOrderItem(Long productId, String productName, int qty, long unitPrice) {
            this(null, productId, productName, qty, unitPrice);
        }
    }

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

    /** prepare에서 예약금과 잔금을 확정한 뒤 결제 시도에만 저장하는 예약 payload. */
    record PreparedBookingPayload(
            Long userId,
            String phone,
            String guestVerificationProof,
            String name,
            Long slotId,
            Long passId,
            DepositPaymentMethod paymentMethod,
            long depositAmount,
            long balanceAmount
    ) implements PaymentPayload {

        public static PreparedBookingPayload fromMember(
                BookingPayload payload, long depositAmount, long balanceAmount) {
            return new PreparedBookingPayload(
                    payload.userId(), null, null, null,
                    payload.slotId(), payload.passId(), payload.paymentMethod(), depositAmount, balanceAmount);
        }
    }

    /** 8회권 구매 payload. 회원 전용 — userId 필수. */
    record PassPayload(Long userId) implements PaymentPayload {}

    /** prepare에서 서버 가격을 확정한 뒤 결제 시도에만 저장하는 8회권 payload. */
    record PreparedPassPayload(Long userId, long totalPrice) implements PaymentPayload {}

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
