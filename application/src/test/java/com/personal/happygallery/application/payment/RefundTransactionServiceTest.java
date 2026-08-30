package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundTransactionServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-08T01:00:00Z"), ZoneOffset.UTC);

    @DisplayName("결제 생성 실패 보상환불은 성공 확정 트랜잭션에서만 예약 혜택을 해제한다")
    @Test
    void markSucceeded_paymentAttemptCompensationReleasesBenefitsAfterStateTransition() {
        Dependencies dependencies = new Dependencies();
        Refund refund = mock(Refund.class);
        PaymentAttempt attempt = mock(PaymentAttempt.class);
        when(dependencies.refundPort.findByIdForUpdate(7L)).thenReturn(Optional.of(refund));
        when(refund.markSucceeded(
                "processing-token", "refund-transaction-key", LocalDateTime.now(CLOCK)))
                .thenReturn(true);
        when(refund.getPaymentAttemptId()).thenReturn(9L);
        when(refund.getBookingId()).thenReturn(null);
        when(refund.getOrderId()).thenReturn(null);
        when(refund.getOrderClaimId()).thenReturn(null);
        when(refund.getPassPurchaseId()).thenReturn(null);
        when(dependencies.refundPort.save(refund)).thenReturn(refund);
        when(dependencies.paymentAttemptReader.findByIdForUpdate(9L))
                .thenReturn(Optional.of(attempt));

        Refund result = dependencies.service().markSucceeded(
                7L, "processing-token", "refund-transaction-key");

        assertThat(result).isSameAs(refund);
        InOrder order = inOrder(attempt, dependencies.benefitReservationService);
        order.verify(attempt).markCompensated();
        order.verify(dependencies.benefitReservationService)
                .release(attempt, LocalDateTime.now(CLOCK));
        verify(dependencies.paymentAttemptStore).save(attempt);
    }

    @DisplayName("보상환불 최종 실패는 결제 혜택 예약을 해제하지 않는다")
    @Test
    void markFailed_paymentAttemptCompensationKeepsBenefitReservation() {
        Dependencies dependencies = new Dependencies();
        Refund refund = mock(Refund.class);
        PaymentAttempt attempt = mock(PaymentAttempt.class);
        when(dependencies.refundPort.findByIdForUpdate(7L)).thenReturn(Optional.of(refund));
        when(refund.markFailed("processing-token", "PG 거절")).thenReturn(true);
        when(refund.getPaymentAttemptId()).thenReturn(9L);
        when(dependencies.refundPort.save(refund)).thenReturn(refund);
        when(dependencies.paymentAttemptReader.findByIdForUpdate(9L))
                .thenReturn(Optional.of(attempt));

        dependencies.service().markFailed(7L, "processing-token", "PG 거절");

        verify(attempt).markCompensationFailed("PG 거절");
        verify(dependencies.benefitReservationService, never())
                .release(attempt, LocalDateTime.now(CLOCK));
    }

    private static final class Dependencies {
        private final RefundPort refundPort = mock(RefundPort.class);
        private final PaymentAttemptReaderPort paymentAttemptReader =
                mock(PaymentAttemptReaderPort.class);
        private final PaymentAttemptStorePort paymentAttemptStore =
                mock(PaymentAttemptStorePort.class);
        private final BookingReaderPort bookingReader = mock(BookingReaderPort.class);
        private final OrderReaderPort orderReader = mock(OrderReaderPort.class);
        private final OrderClaimPort orderClaimPort = mock(OrderClaimPort.class);
        private final PassPurchaseReaderPort passPurchaseReader =
                mock(PassPurchaseReaderPort.class);
        private final CouponRedemptionUseCase couponRedemptionUseCase =
                mock(CouponRedemptionUseCase.class);
        private final RewardBenefitService rewardBenefitService = mock(RewardBenefitService.class);
        private final OrderPaymentBenefitReservationService benefitReservationService =
                mock(OrderPaymentBenefitReservationService.class);
        private final ApplicationEventPublisher eventPublisher =
                mock(ApplicationEventPublisher.class);

        private RefundTransactionService service() {
            return new RefundTransactionService(
                    refundPort,
                    paymentAttemptReader,
                    paymentAttemptStore,
                    bookingReader,
                    orderReader,
                    orderClaimPort,
                    passPurchaseReader,
                    couponRedemptionUseCase,
                    rewardBenefitService,
                    benefitReservationService,
                    eventPublisher,
                    CLOCK);
        }
    }
}
