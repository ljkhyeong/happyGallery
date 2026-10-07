package com.personal.happygallery.application.payment.context;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.FulfillmentPolicy;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.order.OrderItemPricing;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.pass.PassPlan;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.ProductOptionType;
import java.util.List;

/**
 * prepare에서 서버가 확정해 결제 시도에 암호화 저장하는 내부 스냅샷.
 *
 * <p>필수 값 누락은 역직렬화가 아니라 각 fulfiller의 저장 payload 검증에서
 * "결제를 다시 준비해 주세요" 오류로 거절한다.
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
            MadeToOrderConsent madeToOrderConsent,
            OrderPricingSnapshot pricing
    ) implements PreparedPaymentPayload {

        public PreparedOrderPayload {
            FulfillmentPolicy.requireValid(fulfillmentType, shippingAddress);
            if (shippingFee < 0L || (fulfillmentType == FulfillmentType.PICKUP && shippingFee != 0L)) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "수령 방법과 배송비가 일치하지 않습니다.");
            }
        }
    }

    record PreparedOrderItem(
            Long cartItemId,
            Long productId,
            Long productVariantId,
            String productName,
            int qty,
            long unitPrice,
            long basePrice,
            long variantPriceAdjustment,
            long textOptionPriceAdjustment,
            List<PreparedOrderOption> optionSnapshots,
            String specification,
            String careInstructions,
            Integer productionLeadDays,
            ProductType productType,
            OrderItemPricing pricing
    ) {

        public PreparedOrderItem {
            optionSnapshots = optionSnapshots == null ? List.of() : List.copyOf(optionSnapshots);
        }
    }

    record PreparedOrderOption(
            ProductOptionType type,
            String groupName,
            String value,
            long priceAdjustment,
            int sortOrder
    ) {}

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
            int participantCount
    ) implements PreparedPaymentPayload {}

    record PreparedPassPayload(Long userId, long totalPrice, PassPlan plan) implements PreparedPaymentPayload {}
}
