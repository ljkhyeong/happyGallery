package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.LocalDateTime;
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
class OrderPaymentBenefitReservationServiceTest {

    @Mock CouponRedemptionUseCase couponRedemptionUseCase;
    @Mock RewardBenefitService rewardBenefitService;
    @Mock MemberAccountGuard memberAccountGuard;
    @Mock PaymentConfirmAttemptResolver attemptResolver;
    @InjectMocks OrderPaymentBenefitReservationService service;

    @Test
    @DisplayName("PENDING 결제에서는 혜택 예약 해제를 거절한다")
    void release_nonTerminalAttempt_isRejected() {
        PreparedOrderPayload payload = new PreparedOrderPayload(
                1L,
                null,
                null,
                null,
                List.of(new PreparedOrderItem(1L, "상품", 1, 10_000L)),
                false,
                FulfillmentType.PICKUP,
                null,
                0L,
                null);
        PaymentAttempt pending = attempt("pending");

        assertThatThrownBy(() -> service.release(pending, payload, LocalDateTime.now()))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private PaymentAttempt attempt(String suffix) {
        return PaymentAttempt.startForMember(
                suffix + "-benefit-attempt",
                PaymentContext.ORDER,
                10_000L,
                "encrypted-payload",
                1L);
    }
}
