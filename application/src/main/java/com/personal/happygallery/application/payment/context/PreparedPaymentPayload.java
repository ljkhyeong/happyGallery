package com.personal.happygallery.application.payment.context;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.FulfillmentPolicy;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;

/**
 * prepare에서 서버가 확정해 결제 시도에 암호화 저장하는 내부 스냅샷.
 *
 * <p>Jackson subtype 이름은 기존 저장 JSON과의 호환을 위해 변경하지 않는다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = PreparedPaymentPayload.PreparedOrderPayload.class,
                name = "PREPARED_ORDER"),
        @JsonSubTypes.Type(
                value = PreparedPaymentPayload.PreparedBookingPayload.class,
                name = "PREPARED_BOOKING"),
        @JsonSubTypes.Type(
                value = PreparedPaymentPayload.PreparedPassPayload.class,
                name = "PREPARED_PASS")
})
public sealed interface PreparedPaymentPayload {

    /** prepare 당시 결제 주체. 비회원이면 null이다. */
    Long userId();

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
    ) implements PreparedPaymentPayload {

        public PreparedOrderPayload {
            FulfillmentPolicy.requireValid(fulfillmentType, shippingAddress);
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
            long unitPrice,
            String specification,
            String careInstructions,
            Integer productionLeadDays,
            ProductType productType
    ) {

        /** V97 이전 저장 JSON과 테스트 fixture는 구매 조건과 상품 유형이 모두 null이다. */
        public PreparedOrderItem(Long productId, String productName, int qty, long unitPrice) {
            this(null, productId, productName, qty, unitPrice, null, null, null, null);
        }

        /** V97 이전 장바구니 결제 스냅샷 호환 생성자. */
        public PreparedOrderItem(
                Long cartItemId, Long productId, String productName, int qty, long unitPrice) {
            this(cartItemId, productId, productName, qty, unitPrice, null, null, null, null);
        }
    }

    record PreparedBookingPayload(
            Long userId,
            String phone,
            String guestVerificationProof,
            String name,
            Long slotId,
            Long passId,
            DepositPaymentMethod paymentMethod,
            long depositAmount,
            long balanceAmount,
            Integer participantCount
    ) implements PreparedPaymentPayload {

        /** V95 배포 전에 저장된 미확정 결제 시도는 기존 단일 인원 계약으로 복구한다. */
        public int effectiveParticipantCount() {
            return participantCount == null ? 1 : participantCount;
        }
    }

    record PreparedPassPayload(Long userId, long totalPrice) implements PreparedPaymentPayload {}
}
