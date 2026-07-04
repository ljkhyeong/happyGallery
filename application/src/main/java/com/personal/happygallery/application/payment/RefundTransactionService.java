package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.customer.GuestPhoneProtector;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
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
    private final BookingReaderPort bookingReader;
    private final OrderReaderPort orderReader;
    private final GuestPhoneProtector guestPhoneProtector;
    private final ApplicationEventPublisher eventPublisher;

    RefundTransactionService(RefundPort refundPort,
                             BookingReaderPort bookingReader,
                             OrderReaderPort orderReader,
                             GuestPhoneProtector guestPhoneProtector,
                             ApplicationEventPublisher eventPublisher) {
        this.refundPort = refundPort;
        this.bookingReader = bookingReader;
        this.orderReader = orderReader;
        this.guestPhoneProtector = guestPhoneProtector;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public RefundCall loadRefundCall(Long refundId) {
        Refund refund = findRefund(refundId);
        return new RefundCall(refund.getId(), refund.getPaymentKey(), refund.getAmount());
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
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
        publishRefundSucceededNotification(savedRefund);
        return savedRefund;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund markFailed(Long refundId, String reason) {
        Refund refund = findRefund(refundId);
        refund.markFailed(reason);
        return refundPort.save(refund);
    }

    private Refund findRefund(Long refundId) {
        return refundPort.findById(refundId)
                .orElseThrow(NotFoundException.supplier("환불"));
    }

    private void publishRefundSucceededNotification(Refund refund) {
        try {
            if (refund.getBookingId() != null) {
                publishBookingRefunded(refund.getBookingId());
            } else if (refund.getOrderId() != null) {
                publishOrderRefunded(refund.getOrderId());
            }
        } catch (Exception e) {
            log.warn("환불 성공 알림 발행 실패 [refundId={}]", refund.getId(), e);
        }
    }

    private void publishBookingRefunded(Long bookingId) {
        bookingReader.findById(bookingId).ifPresent(booking -> {
            if (booking.getUserId() != null) {
                eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                        booking.getUserId(), NotificationEventType.DEPOSIT_REFUNDED));
            } else if (booking.getGuest() != null) {
                Guest guest = booking.getGuest();
                eventPublisher.publishEvent(NotificationRequestedEvent.forGuestWithContact(
                        guest.getId(),
                        guestPhoneProtector.decrypt(guest),
                        guest.getName(),
                        NotificationEventType.DEPOSIT_REFUNDED));
            }
        });
    }

    private void publishOrderRefunded(Long orderId) {
        orderReader.findById(orderId).ifPresent(order -> {
            if (order.getUserId() != null) {
                eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                        order.getUserId(), NotificationEventType.ORDER_REFUNDED));
            } else if (order.getGuestId() != null) {
                eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(
                        order.getGuestId(), NotificationEventType.ORDER_REFUNDED));
            }
        });
    }

    record RefundCall(Long refundId, String paymentKey, long amount) {}
}
