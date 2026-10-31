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
 * <p>주문 혜택 금액·상품 유형·이용권 종류 같은 구조적 필수 값과 수령 방식·배송지·배송비의 일관성은
 * 각 record의 compact 생성자가 검사한다. 저장 JSON을 역직렬화하는 시점에 기본값으로 보정하지 않고
 * {@link ErrorCode#INVALID_INPUT}으로 거절하므로 confirm뿐 아니라 혜택 해제를 위해 payload를 읽는
 * 만료·종료·대사 같은 경로도 같은 검사를 거친다. 금액·상품명 같은 나머지 불변식과 결제 시도 금액 대조는
 * 각 fulfiller의 {@code validateStoredPayload}가 맡는다.
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
            if (pricing == null) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "저장된 주문 혜택 금액이 없습니다. 결제를 다시 준비해 주세요.");
            }
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
            // pricing은 혜택 배분 전 준비 단계에서 비어 있을 수 있어 이행 검증에서 확인한다.
            if (productType == null) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "저장된 주문 상품 정보가 없습니다. 결제를 다시 준비해 주세요.");
            }
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

    record PreparedPassPayload(Long userId, long totalPrice, PassPlan plan) implements PreparedPaymentPayload {

        public PreparedPassPayload {
            if (plan == null) {
                throw new HappyGalleryException(
                        ErrorCode.INVALID_INPUT, "이용권 종류 정보가 없습니다. 결제를 다시 준비해 주세요.");
            }
        }
    }
}
