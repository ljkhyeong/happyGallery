package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.customer.GuestPhoneProtector;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class RefundTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RefundTransactionService.class);
    static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
    private static final int MAX_FAILURE_REASON_LENGTH = 500;
    private static final String MISSING_PAYMENT_KEY_REASON =
            "paymentKey가 없어 PG 환불을 실행할 수 없습니다.";

    private final RefundPort refundPort;
    private final PaymentAttemptReaderPort paymentAttemptReader;
    private final PaymentAttemptStorePort paymentAttemptStore;
    private final BookingReaderPort bookingReader;
    private final OrderReaderPort orderReader;
    private final GuestPhoneProtector guestPhoneProtector;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    RefundTransactionService(RefundPort refundPort,
                             PaymentAttemptReaderPort paymentAttemptReader,
                             PaymentAttemptStorePort paymentAttemptStore,
                             BookingReaderPort bookingReader,
                             OrderReaderPort orderReader,
                             GuestPhoneProtector guestPhoneProtector,
                             ApplicationEventPublisher eventPublisher,
                             Clock clock) {
        this.refundPort = refundPort;
        this.paymentAttemptReader = paymentAttemptReader;
        this.paymentAttemptStore = paymentAttemptStore;
        this.bookingReader = bookingReader;
        this.orderReader = orderReader;
        this.guestPhoneProtector = guestPhoneProtector;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundCall claimRefundCall(Long refundId) {
        Refund refund = findRefundForUpdate(refundId);
        LocalDateTime now = LocalDateTime.now(clock);
        String processingToken = refund.startProcessing(now, now.minus(PROCESSING_TIMEOUT));
        if (processingToken == null) {
            return RefundCall.completed(refund);
        }
        if (refund.getPaymentKey() == null || refund.getPaymentKey().isBlank()) {
            refund.markFailed(processingToken, MISSING_PAYMENT_KEY_REASON);
            Refund failedRefund = refundPort.save(refund);
            markPaymentAttemptCompensationFailed(failedRefund, MISSING_PAYMENT_KEY_REASON);
            return RefundCall.completed(failedRefund);
        }
        return RefundCall.ready(
                refund.getId(), refund.getPaymentKey(), refund.getAmount(), refund.getIdempotencyKey(), processingToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requestManualRetry(Long refundId) {
        Refund refund = findRefundForUpdate(refundId);
        try {
            refund.requestRetry(LocalDateTime.now(clock));
        } catch (IllegalStateException e) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "조치 필요 상태 환불만 재시도 가능합니다. (현재: " + refund.getStatus() + ")");
        }
        refundPort.save(refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markSucceeded(Long refundId, String processingToken, String refundTransactionKey) {
        Refund refund = findRefundForUpdate(refundId);
        if (!refund.markSucceeded(processingToken, refundTransactionKey)) {
            return refund;
        }
        Refund savedRefund = refundPort.save(refund);
        markPaymentAttemptCompensated(savedRefund);
        publishRefundSucceededNotification(savedRefund);
        return savedRefund;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markFailed(Long refundId, String processingToken, String reason) {
        Refund refund = findRefundForUpdate(refundId);
        String resolvedReason = failureReason(reason);
        if (!refund.markFailed(processingToken, resolvedReason)) {
            return refund;
        }
        Refund savedRefund = refundPort.save(refund);
        markPaymentAttemptCompensationFailed(savedRefund, resolvedReason);
        return savedRefund;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markRetryable(Long refundId, String processingToken, String reason) {
        Refund refund = findRefundForUpdate(refundId);
        if (!refund.markRetryable(
                processingToken,
                failureReason(reason),
                LocalDateTime.now(clock).plus(RETRY_DELAY))) {
            return refund;
        }
        return refundPort.save(refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markReconciliationRequired(Long refundId, String processingToken, String reason) {
        Refund refund = findRefundForUpdate(refundId);
        if (!refund.markReconciliationRequired(
                processingToken,
                failureReason(reason),
                LocalDateTime.now(clock).plus(RETRY_DELAY))) {
            return refund;
        }
        return refundPort.save(refund);
    }

    private Refund findRefundForUpdate(Long refundId) {
        return refundPort.findByIdForUpdate(refundId)
                .orElseThrow(NotFoundException.supplier("환불"));
    }

    private String failureReason(String reason) {
        String resolved = reason == null || reason.isBlank() ? "PG 환불 처리 결과를 확인할 수 없습니다." : reason;
        return resolved.length() <= MAX_FAILURE_REASON_LENGTH
                ? resolved
                : resolved.substring(0, MAX_FAILURE_REASON_LENGTH);
    }

    private void markPaymentAttemptCompensated(Refund refund) {
        if (refund.getPaymentAttemptId() == null) {
            return;
        }
        var attempt = paymentAttemptReader.findByIdForUpdate(refund.getPaymentAttemptId())
                .orElseThrow(NotFoundException.supplier("결제 시도"));
        attempt.markCompensated();
        paymentAttemptStore.save(attempt);
    }

    private void markPaymentAttemptCompensationFailed(Refund refund, String reason) {
        if (refund.getPaymentAttemptId() == null) {
            return;
        }
        var attempt = paymentAttemptReader.findByIdForUpdate(refund.getPaymentAttemptId())
                .orElseThrow(NotFoundException.supplier("결제 시도"));
        attempt.markCompensationFailed(reason);
        paymentAttemptStore.save(attempt);
    }

    private void publishRefundSucceededNotification(Refund refund) {
        try {
            if (refund.getBookingId() != null) {
                publishBookingRefunded(refund);
            } else if (refund.getOrderId() != null) {
                publishOrderRefunded(refund);
            }
        } catch (Exception e) {
            log.warn("환불 성공 알림 발행 실패 [refundId={}]", refund.getId(), e);
        }
    }

    private void publishBookingRefunded(Refund refund) {
        bookingReader.findById(refund.getBookingId()).ifPresent(booking -> {
            if (booking.getUserId() != null) {
                eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                        booking.getUserId(),
                        NotificationEventType.DEPOSIT_REFUNDED,
                        "REFUND",
                        refund.getId()));
            } else {
                Guest guest = booking.getGuest();
                eventPublisher.publishEvent(NotificationRequestedEvent.forGuestWithContact(
                        guest.getId(),
                        guestPhoneProtector.decrypt(guest),
                        guest.getName(),
                        NotificationEventType.DEPOSIT_REFUNDED,
                        "REFUND",
                        refund.getId()));
            }
        });
    }

    private void publishOrderRefunded(Refund refund) {
        orderReader.findById(refund.getOrderId()).ifPresent(order -> {
            if (order.getUserId() != null) {
                eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                        order.getUserId(),
                        NotificationEventType.ORDER_REFUNDED,
                        "REFUND",
                        refund.getId()));
            } else {
                eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(
                        order.getGuestId(),
                        NotificationEventType.ORDER_REFUNDED,
                        "REFUND",
                        refund.getId()));
            }
        });
    }

    record RefundCall(Long refundId, String paymentKey, long amount,
                      String idempotencyKey, String processingToken, Refund completedRefund) {

        static RefundCall ready(Long refundId, String paymentKey, long amount,
                                String idempotencyKey, String processingToken) {
            return new RefundCall(refundId, paymentKey, amount, idempotencyKey, processingToken, null);
        }

        static RefundCall completed(Refund refund) {
            return new RefundCall(
                    refund.getId(), null, refund.getAmount(), refund.getIdempotencyKey(), null, refund);
        }

        boolean readyForPgCall() {
            return completedRefund == null;
        }
    }
}
