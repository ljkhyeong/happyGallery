package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.order.FulfillmentPolicy;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import java.util.List;

/**
 * web prepare DTO에서 변환된 context별 application command payload.
 *
 * <p>prepare가 만든 서버 확정 스냅샷은
 * {@link com.personal.happygallery.application.payment.context.PreparedPaymentPayload}로 분리해
 * command 타입과 암호화 저장 타입이 섞이지 않게 한다.
 */
public sealed interface PaymentPayload {

    /** prepare 당시 결제 주체. 비회원이면 null이다. */
    Long userId();

    default PolicyAcceptance policyAcceptance() {
        return null;
    }

    /**
     * 주문 결제 payload.
     *
     * <p>{@code phone/code/name}이 채워지면 비회원 휴대폰 인증 경로,
     * {@code userId}가 채워지면 회원 경로다. {@code cartCheckout=true}이면
     * 클라이언트의 {@code items} 대신 서버 장바구니의 구매 가능한 항목을 사용한다.
     * {@code expectedCartVersion}이 있으면 조회한 장바구니 스냅샷과 일치할 때만 준비한다.
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
            boolean madeToOrderConsent,
            PolicyAcceptance policyAcceptance,
            String expectedCartVersion,
            Long issuedCouponId,
            long rewardAmount
    ) implements PaymentPayload {

        public OrderPayload {
            FulfillmentPolicy.requireValid(fulfillmentType, shippingAddress);
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
                    fulfillmentType, shippingAddress, null, false, null, null, null, 0L);
        }

        public OrderPayload(Long userId,
                            String phone,
                            String verificationCode,
                            String name,
                            List<OrderItemRef> items,
                            boolean cartCheckout,
                            FulfillmentType fulfillmentType,
                            ShippingAddress shippingAddress,
                            String madeToOrderConsentVersion,
                            boolean madeToOrderConsent) {
            this(userId, phone, verificationCode, name, items, cartCheckout,
                    fulfillmentType, shippingAddress,
                    madeToOrderConsentVersion, madeToOrderConsent, null, null, null, 0L);
        }

        public OrderPayload(Long userId,
                            String phone,
                            String verificationCode,
                            String name,
                            List<OrderItemRef> items,
                            boolean cartCheckout,
                            FulfillmentType fulfillmentType,
                            ShippingAddress shippingAddress,
                            String madeToOrderConsentVersion,
                            boolean madeToOrderConsent,
                            PolicyAcceptance policyAcceptance) {
            this(userId, phone, verificationCode, name, items, cartCheckout,
                    fulfillmentType, shippingAddress,
                    madeToOrderConsentVersion, madeToOrderConsent, policyAcceptance, null, null, 0L);
        }

        public OrderPayload(Long userId,
                            String phone,
                            String verificationCode,
                            String name,
                            List<OrderItemRef> items,
                            boolean cartCheckout,
                            FulfillmentType fulfillmentType,
                            ShippingAddress shippingAddress,
                            String madeToOrderConsentVersion,
                            boolean madeToOrderConsent,
                            PolicyAcceptance policyAcceptance,
                            String expectedCartVersion) {
            this(userId, phone, verificationCode, name, items, cartCheckout,
                    fulfillmentType, shippingAddress,
                    madeToOrderConsentVersion, madeToOrderConsent, policyAcceptance,
                    expectedCartVersion, null, 0L);
        }
    }

    record OrderItemRef(
            Long productId,
            Long productVariantId,
            List<OrderTextInput> textInputs,
            int qty
    ) {
        public OrderItemRef {
            textInputs = textInputs == null ? List.of() : List.copyOf(textInputs);
        }

        public OrderItemRef(Long productId, int qty) {
            this(productId, null, List.of(), qty);
        }
    }

    record OrderTextInput(String groupKey, String value) {}

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
            DepositPaymentMethod paymentMethod,
            int participantCount,
            PolicyAcceptance policyAcceptance
    ) implements PaymentPayload {

        public BookingPayload(Long userId,
                              String phone,
                              String verificationCode,
                              String name,
                              Long slotId,
                              Long passId,
                              DepositPaymentMethod paymentMethod) {
            this(userId, phone, verificationCode, name, slotId, passId, paymentMethod, 1, null);
        }

        public BookingPayload(Long userId,
                              String phone,
                              String verificationCode,
                              String name,
                              Long slotId,
                              Long passId,
                              DepositPaymentMethod paymentMethod,
                              PolicyAcceptance policyAcceptance) {
            this(userId, phone, verificationCode, name, slotId, passId,
                    paymentMethod, 1, policyAcceptance);
        }
    }

    /** 8회권 구매 payload. 회원 전용 — userId 필수. */
    record PassPayload(Long userId) implements PaymentPayload {}
}
