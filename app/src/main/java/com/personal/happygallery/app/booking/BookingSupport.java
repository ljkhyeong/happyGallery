package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.app.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class BookingSupport {

    private final BookingReaderPort bookingReaderPort;
    private final BookingHistoryPort bookingHistoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final GuestTokenService guestTokenService;

    BookingSupport(BookingReaderPort bookingReaderPort,
                   BookingHistoryPort bookingHistoryPort,
                   ApplicationEventPublisher eventPublisher,
                   GuestTokenService guestTokenService) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingHistoryPort = bookingHistoryPort;
        this.eventPublisher = eventPublisher;
        this.guestTokenService = guestTokenService;
    }

    Booking findByToken(Long bookingId, String rawAccessToken) {
        String tokenHash = guestTokenService.resolveTokenHash(rawAccessToken);
        return bookingReaderPort.findDetailByIdAndAccessToken(bookingId, tokenHash)
                .orElseThrow(NotFoundException.supplier("예약"));
    }

    Booking findByIdAndUserId(Long bookingId, Long userId) {
        return bookingReaderPort.findById(bookingId)
                .filter(b -> Objects.equals(b.getUserId(), userId))
                .orElseThrow(NotFoundException.supplier("예약"));
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
            eventPublisher.publishEvent(NotificationRequestedEvent.forUser(booking.getUserId(), eventType));
        } else if (booking.getGuest() != null) {
            eventPublisher.publishEvent(NotificationRequestedEvent.forGuestWithContact(
                    booking.getGuest().getId(),
                    booking.getGuest().getPhone(),
                    booking.getGuest().getName(),
                    eventType));
        }
    }
}
