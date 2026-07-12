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
import com.personal.happygallery.domain.payment.RefundStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class RefundTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RefundTransactionService.class);

    private final RefundPort refundPort;
    private final PaymentAttemptReaderPort paymentAttemptReader;
    private final PaymentAttemptStorePort paymentAttemptStore;
    private final BookingReaderPort bookingReader;
    private final OrderReaderPort orderReader;
    private final GuestPhoneProtector guestPhoneProtector;
    private final ApplicationEventPublisher eventPublisher;

    RefundTransactionService(RefundPort refundPort,
                             PaymentAttemptReaderPort paymentAttemptReader,
                             PaymentAttemptStorePort paymentAttemptStore,
                             BookingReaderPort bookingReader,
                             OrderReaderPort orderReader,
                             GuestPhoneProtector guestPhoneProtector,
                             ApplicationEventPublisher eventPublisher) {
        this.refundPort = refundPort;
        this.paymentAttemptReader = paymentAttemptReader;
        this.paymentAttemptStore = paymentAttemptStore;
        this.bookingReader = bookingReader;
        this.orderReader = orderReader;
        this.guestPhoneProtector = guestPhoneProtector;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundCall prepareRefundCall(Long refundId, String missingPaymentKeyReason) {
        Refund refund = findRefund(refundId);
        if (refund.getPaymentKey() == null || refund.getPaymentKey().isBlank()) {
            refund.markFailed(missingPaymentKeyReason);
            Refund failedRefund = refundPort.save(refund);
            markPaymentAttemptCompensationFailed(failedRefund, missingPaymentKeyReason);
            return RefundCall.failed(failedRefund);
        }
        return RefundCall.ready(
                refund.getId(), refund.getPaymentKey(), refund.getAmount(), refund.getIdempotencyKey());
    }

    @Transactional(readOnly = true)
    public Refund find(Long refundId) {
        return findRefund(refundId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateRetryable(Long refundId) {
        Refund refund = findRefund(refundId);
        if (refund.getStatus() != RefundStatus.FAILED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "FAILED 상태 환불만 재시도 가능합니다. (현재: " + refund.getStatus() + ")");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markSucceeded(Long refundId, String refundTransactionKey) {
        Refund refund = findRefund(refundId);
        refund.markSucceeded(refundTransactionKey);
        Refund savedRefund = refundPort.save(refund);
        markPaymentAttemptCompensated(savedRefund);
        publishRefundSucceededNotification(savedRefund);
        return savedRefund;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markFailed(Long refundId, String reason) {
        Refund refund = findRefund(refundId);
        refund.markFailed(reason);
        Refund savedRefund = refundPort.save(refund);
        markPaymentAttemptCompensationFailed(savedRefund, reason);
        return savedRefund;
    }

    private Refund findRefund(Long refundId) {
        return refundPort.findById(refundId)
                .orElseThrow(NotFoundException.supplier("환불"));
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
            } else if (booking.getGuest() != null) {
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
            } else if (order.getGuestId() != null) {
                eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(
                        order.getGuestId(),
                        NotificationEventType.ORDER_REFUNDED,
                        "REFUND",
                        refund.getId()));
            }
        });
    }

    record RefundCall(Long refundId, String paymentKey, long amount,
                      String idempotencyKey, Refund failedRefund) {

        static RefundCall ready(Long refundId, String paymentKey, long amount, String idempotencyKey) {
            return new RefundCall(refundId, paymentKey, amount, idempotencyKey, null);
        }

        static RefundCall failed(Refund refund) {
            return new RefundCall(
                    refund.getId(), null, refund.getAmount(), refund.getIdempotencyKey(), refund);
        }

        boolean failedBeforePgCall() {
            return failedRefund != null;
        }
    }
}
