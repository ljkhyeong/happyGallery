package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.token.AccessTokenHasher;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class BookingSupport {

    private final BookingReaderPort bookingReaderPort;
    private final BookingHistoryPort bookingHistoryPort;
    private final NotificationService notificationService;

    BookingSupport(BookingReaderPort bookingReaderPort,
                   BookingHistoryPort bookingHistoryPort,
                   NotificationService notificationService) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingHistoryPort = bookingHistoryPort;
        this.notificationService = notificationService;
    }

    Booking findByToken(Long bookingId, String rawAccessToken) {
        String tokenHash = AccessTokenHasher.hash(rawAccessToken);
        return bookingReaderPort.findDetailByIdAndAccessToken(bookingId, tokenHash)
                .orElseThrow(() -> new NotFoundException("예약"));
    }

    Booking findByIdAndUserId(Long bookingId, Long userId) {
        return bookingReaderPort.findById(bookingId)
                .filter(b -> Objects.equals(b.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("예약"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void recordHistory(Booking booking, BookingHistoryAction action,
                       Slot oldSlot, Slot newSlot, String actor, String reason) {
        bookingHistoryPort.save(
                new BookingHistory(booking, action, oldSlot, newSlot, actor, reason));
    }

    /** booking 의 guest/member 를 자동 판별하여 알림을 발송한다. */
    void notifyBooker(Booking booking, NotificationEventType eventType) {
        if (booking.getUserId() != null) {
            notificationService.notifyByUserId(booking.getUserId(), eventType);
        } else if (booking.getGuest() != null) {
            notificationService.notifyGuest(
                    booking.getGuest().getId(),
                    booking.getGuest().getPhone(),
                    booking.getGuest().getName(),
                    eventType);
        }
    }
}
