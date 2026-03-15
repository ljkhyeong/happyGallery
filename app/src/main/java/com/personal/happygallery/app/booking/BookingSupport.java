package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.util.Objects;
import org.springframework.stereotype.Component;

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

    Booking findByToken(Long bookingId, String accessToken) {
        return bookingReaderPort.findDetailByIdAndAccessToken(bookingId, accessToken)
                .orElseThrow(() -> new NotFoundException("예약"));
    }

    Booking findByIdAndUserId(Long bookingId, Long userId) {
        return bookingReaderPort.findById(bookingId)
                .filter(b -> Objects.equals(b.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("예약"));
    }

    void recordHistory(Booking booking, BookingHistoryAction action,
                       Slot oldSlot, Slot newSlot, String actor, String reason) {
        bookingHistoryPort.save(
                new BookingHistory(booking, action, oldSlot, newSlot, actor, reason));
    }

    void notifyBookingGuest(Booking booking, NotificationEventType eventType) {
        if (booking.getGuest() == null) {
            return;
        }
        notificationService.notifyGuest(
                booking.getGuest().getId(),
                booking.getGuest().getPhone(),
                booking.getGuest().getName(),
                eventType);
    }

    void notifyBookingUser(Booking booking, NotificationEventType eventType) {
        if (booking.getUserId() == null) {
            return;
        }
        notificationService.notifyByUserId(booking.getUserId(), eventType);
    }
}
