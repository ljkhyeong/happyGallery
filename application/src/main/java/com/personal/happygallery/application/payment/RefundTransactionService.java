package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.payment.PaymentFailureReason;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class RefundTransactionService {

    static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
    private static final String MISSING_PAYMENT_KEY_REASON =
            "paymentKey가 없어 PG 환불을 실행할 수 없습니다.";
    private static final String UNKNOWN_REFUND_RESULT_REASON =
            "PG 환불 처리 결과를 확인할 수 없습니다.";

    private final RefundPort refundPort;
    private final PaymentAttemptReaderPort paymentAttemptReader;
    private final BookingReaderPort bookingReader;
    private final OrderReaderPort orderReader;
    private final OrderClaimPort orderClaimPort;
    private final PassPurchaseReaderPort passPurchaseReader;
    private final CouponRedemptionUseCase couponRedemptionUseCase;
    private final RewardBenefitService rewardBenefitService;
    private final OrderPaymentBenefitReservationService benefitReservationService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    RefundTransactionService(RefundPort refundPort,
                             PaymentAttemptReaderPort paymentAttemptReader,
                             BookingReaderPort bookingReader,
                             OrderReaderPort orderReader,
                             OrderClaimPort orderClaimPort,
                             PassPurchaseReaderPort passPurchaseReader,
                             CouponRedemptionUseCase couponRedemptionUseCase,
                             RewardBenefitService rewardBenefitService,
                             OrderPaymentBenefitReservationService benefitReservationService,
                             ApplicationEventPublisher eventPublisher,
                             Clock clock) {
        this.refundPort = refundPort;
        this.paymentAttemptReader = paymentAttemptReader;
        this.bookingReader = bookingReader;
        this.orderReader = orderReader;
        this.orderClaimPort = orderClaimPort;
        this.passPurchaseReader = passPurchaseReader;
        this.couponRedemptionUseCase = couponRedemptionUseCase;
        this.rewardBenefitService = rewardBenefitService;
        this.benefitReservationService = benefitReservationService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundCall claimRefundCall(Long refundId) {
        return claimRefundCall(refundId, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundCall claimRefundCallForRecovery(Long refundId) {
        return claimRefundCall(refundId, true);
    }

    private RefundCall claimRefundCall(Long refundId, boolean recovery) {
        Refund refund = findRefundForUpdate(refundId);
        RefundStatus statusBeforeClaim = refund.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);
        String processingToken = refund.startProcessing(now, now.minus(PROCESSING_TIMEOUT));
        if (processingToken == null) {
            return new RefundCall.Skipped(refund);
        }
        if (recovery) {
            refund.recordRecoveryAttempt(now);
        }
        if (!refund.requiresPgCancellation()) {
            return new RefundCall.LocalOnlyRequired(refund.getId(), processingToken);
        }
        if (!StringUtils.hasText(refund.getPaymentKey())) {
            refund.markFailed(processingToken, MISSING_PAYMENT_KEY_REASON);
            markPaymentAttemptCompensationFailed(refund, MISSING_PAYMENT_KEY_REASON);
            return new RefundCall.Skipped(refund);
        }
        if (statusBeforeClaim == RefundStatus.RECONCILIATION_REQUIRED) {
            return new RefundCall.LookupRequired(
                    refund.getId(),
                    refund.getPaymentKey(),
                    refund.getAmount(),
                    refund.getIdempotencyKey(),
                    processingToken);
        }
        return new RefundCall.CancelRequired(
                refund.getId(), refund.getPaymentKey(), refund.getAmount(), refund.getIdempotencyKey(), processingToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requestManualRetry(Long refundId) {
        findRefundForUpdate(refundId).requestRetry(LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markSucceeded(Long refundId, String processingToken, String refundTransactionKey) {
        Refund refund = findRefundForUpdate(refundId);
        if (!refund.markSucceeded(processingToken, refundTransactionKey, LocalDateTime.now(clock))) {
            return refund;
        }
        return completeSuccessfulRefund(refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markLocallySucceeded(Long refundId, String processingToken) {
        Refund refund = findRefundForUpdate(refundId);
        if (!refund.markLocallySucceeded(processingToken, LocalDateTime.now(clock))) {
            return refund;
        }
        return completeSuccessfulRefund(refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markFailed(Long refundId, String processingToken, String reason) {
        Refund refund = findRefundForUpdate(refundId);
        String resolvedReason = PaymentFailureReason.normalize(reason, UNKNOWN_REFUND_RESULT_REASON);
        if (refund.markFailed(processingToken, resolvedReason)) {
            markPaymentAttemptCompensationFailed(refund, resolvedReason);
        }
        return refund;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markRetryable(Long refundId, String processingToken, String reason) {
        Refund refund = findRefundForUpdate(refundId);
        refund.markRetryable(
                processingToken,
                PaymentFailureReason.normalize(reason, UNKNOWN_REFUND_RESULT_REASON),
                LocalDateTime.now(clock).plus(RETRY_DELAY));
        return refund;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markReconciliationRequired(Long refundId, String processingToken, String reason) {
        Refund refund = findRefundForUpdate(refundId);
        refund.markReconciliationRequired(
                processingToken,
                PaymentFailureReason.normalize(reason, UNKNOWN_REFUND_RESULT_REASON),
                LocalDateTime.now(clock).plus(RETRY_DELAY));
        return refund;
    }

    private Refund findRefundForUpdate(Long refundId) {
        return refundPort.findByIdForUpdate(refundId)
                .orElseThrow(NotFoundException.supplier("환불"));
    }

    private void markPaymentAttemptCompensated(Refund refund) {
        if (refund.getPaymentAttemptId() == null) {
            return;
        }
        var attempt = paymentAttemptReader.findByIdForUpdate(refund.getPaymentAttemptId())
                .orElseThrow(NotFoundException.supplier("결제 시도"));
        attempt.markCompensated();
        benefitReservationService.release(attempt, LocalDateTime.now(clock));
    }

    private Refund completeSuccessfulRefund(Refund refund) {
        markPaymentAttemptCompensated(refund);
        restoreOrderBenefits(refund);
        markOrderClaimCompleted(refund);
        publishRefundSucceededNotification(refund);
        return refund;
    }

    private void restoreOrderBenefits(Refund refund) {
        if (refund.getOrderId() == null) {
            return;
        }
        var order = orderReader.findByIdForUpdate(refund.getOrderId())
                .orElseThrow(NotFoundException.supplier("주문"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (order.getUserId() != null) {
            rewardBenefitService.restoreUsed(
                    order.getId(),
                    refund.getRewardRestoreAmount(),
                    "reward:restore:refund:" + refund.getId(),
                    now);
            rewardBenefitService.revokeEarned(
                    order.getUserId(),
                    order.getId(),
                    refund.getRewardRevokeAmount(),
                    "reward:revoke:refund:" + refund.getId());
        }
        if (refund.isRestoreCoupon()) {
            couponRedemptionUseCase.restoreAfterFullCancellation(
                    order.getIssuedCouponId(), order.getId(), now);
        }
    }

    private void markOrderClaimCompleted(Refund refund) {
        if (refund.getOrderClaimId() == null) {
            return;
        }
        var claim = orderClaimPort.findByIdForUpdate(refund.getOrderClaimId())
                .orElseThrow(NotFoundException.supplier("주문 클레임"));
        claim.completeRefund(LocalDateTime.now(clock));
    }

    private void markPaymentAttemptCompensationFailed(Refund refund, String reason) {
        if (refund.getPaymentAttemptId() == null) {
            return;
        }
        var attempt = paymentAttemptReader.findByIdForUpdate(refund.getPaymentAttemptId())
                .orElseThrow(NotFoundException.supplier("결제 시도"));
        attempt.markCompensationFailed(reason);
    }

    private void publishRefundSucceededNotification(Refund refund) {
        if (refund.getBookingId() != null) {
            bookingReader.findById(refund.getBookingId()).ifPresent(booking -> publishRefunded(
                    booking.getUserId(),
                    booking.getGuest() == null ? null : booking.getGuest().getId(),
                    NotificationEventType.DEPOSIT_REFUNDED,
                    refund.getId()));
        } else if (refund.getOrderId() != null) {
            orderReader.findById(refund.getOrderId()).ifPresent(order -> publishRefunded(
                    order.getUserId(), order.getGuestId(), NotificationEventType.ORDER_REFUNDED, refund.getId()));
        } else if (refund.getPassPurchaseId() != null) {
            // 이용권은 회원 전용이라 비회원 수신자가 없다.
            passPurchaseReader.findById(refund.getPassPurchaseId()).ifPresent(pass -> publishRefunded(
                    pass.getUserId(), null, NotificationEventType.PASS_REFUNDED, refund.getId()));
        }
    }

    /** 회원이면 회원, 아니면 비회원 수신자로 환불 완료 알림을 요청한다. */
    private void publishRefunded(Long userId, Long guestId, NotificationEventType eventType, Long refundId) {
        eventPublisher.publishEvent(NotificationRequestedEvent.forRecipient(
                userId, guestId, eventType, NotificationAggregateType.REFUND, refundId));
    }

    sealed interface RefundCall permits RefundCall.CancelRequired, RefundCall.LookupRequired,
            RefundCall.LocalOnlyRequired, RefundCall.Skipped {

        record CancelRequired(Long refundId,
                              String paymentKey,
                              long amount,
                              String idempotencyKey,
                              String processingToken) implements RefundCall {}

        record LookupRequired(Long refundId,
                              String paymentKey,
                              long amount,
                              String idempotencyKey,
                              String processingToken) implements RefundCall {}

        record LocalOnlyRequired(Long refundId, String processingToken) implements RefundCall {}

        record Skipped(Refund refund) implements RefundCall {}
    }
}
