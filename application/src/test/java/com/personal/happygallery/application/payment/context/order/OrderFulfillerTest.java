package com.personal.happygallery.application.payment.context.order;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.personal.happygallery.support.TestFixtures.preparedReadyStockItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OrderFulfillerTest {

    @Mock VerifiedGuestResolver verifiedGuestResolver;
    @Mock OrderService orderService;
    @Mock CartUseCase cartUseCase;
    @Mock CouponRedemptionUseCase couponRedemptionUseCase;
    @Mock RewardBenefitService rewardBenefitService;
    @Mock Clock clock;
    @InjectMocks OrderFulfiller fulfiller;

    @Test
    @DisplayName("저장 payload 품목 합계가 결제 상한을 넘으면 노출된 산술 예외 대신 도메인 입력 오류로 거절한다")
    void validateStoredPayload_amountOverflowIsNormalized() {
        PreparedOrderPayload payload = new PreparedOrderPayload(
                1L,
                null,
                null,
                null,
                List.of(
                        preparedReadyStockItem(1L, "상한 상품", 1, PaymentAmountPolicy.MAX_AMOUNT),
                        preparedReadyStockItem(2L, "초과 상품", 1, 1L)),
                false,
                FulfillmentType.PICKUP,
                null,
                0L,
                null,
                OrderPricingSnapshot.fullPrice(PaymentAmountPolicy.MAX_AMOUNT, 0L));
        PaymentAttempt attempt = PaymentAttempt.startForMember(
                "stored-payload-overflow",
                PaymentContext.ORDER,
                PaymentAmountPolicy.MAX_AMOUNT,
                "encrypted-payload",
                1L);

        assertThatThrownBy(() -> fulfiller.validateStoredPayload(attempt, payload))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("상품 유형이 빠진 저장 payload는 기성품으로 보정하지 않고 새 prepare를 요구한다")
    void storedItem_missingProductType_isRejected() {
        PreparedOrderItem prepared = preparedReadyStockItem(1L, "상품", 1, 10_000L);

        assertThatThrownBy(() -> new PreparedOrderItem(
                prepared.cartItemId(), prepared.productId(), prepared.productVariantId(),
                prepared.productName(), prepared.qty(), prepared.unitPrice(), prepared.basePrice(),
                prepared.variantPriceAdjustment(), prepared.textOptionPriceAdjustment(),
                prepared.optionSnapshots(), prepared.specification(), prepared.careInstructions(),
                prepared.productionLeadDays(), null, prepared.pricing()))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
