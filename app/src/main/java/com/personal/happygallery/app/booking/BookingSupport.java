package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import org.springframework.stereotype.Component;

@Component
class BookingSupport {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final NotificationService notificationService;

    BookingSupport(BookingRepository bookingRepository,
                   BookingHistoryRepository bookingHistoryRepository,
                   NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.notificationService = notificationService;
    }

    Booking findByToken(Long bookingId, String accessToken) {
        return bookingRepository.findByIdAndAccessToken(bookingId, accessToken)
                .orElseThrow(() -> new NotFoundException("예약"));
    }

    void recordHistory(Booking booking, BookingHistoryAction action,
                       Slot oldSlot, Slot newSlot, String actor, String reason) {
        bookingHistoryRepository.save(
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
}
