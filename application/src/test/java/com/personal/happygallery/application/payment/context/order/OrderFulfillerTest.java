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
                        new PreparedOrderItem(
                                1L, "상한 상품", 1, PaymentAmountPolicy.MAX_AMOUNT),
                        new PreparedOrderItem(2L, "초과 상품", 1, 1L)),
                false,
                FulfillmentType.PICKUP,
                null,
                0L,
                null);
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
}
