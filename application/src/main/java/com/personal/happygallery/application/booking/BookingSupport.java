package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.customer.GuestPhoneProtector;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.NotFoundException;
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
    private final GuestPhoneProtector guestPhoneProtector;

    BookingSupport(BookingReaderPort bookingReaderPort,
                   BookingHistoryPort bookingHistoryPort,
                   ApplicationEventPublisher eventPublisher,
                   GuestTokenService guestTokenService,
                   GuestPhoneProtector guestPhoneProtector) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingHistoryPort = bookingHistoryPort;
        this.eventPublisher = eventPublisher;
        this.guestTokenService = guestTokenService;
        this.guestPhoneProtector = guestPhoneProtector;
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

    /** 예약 트랜잭션 안에서 guest/member 알림 요청을 outbox 이벤트로 발행한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    void notifyBooker(Booking booking, NotificationEventType eventType) {
        if (booking.getUserId() != null) {
            eventPublisher.publishEvent(bookerEventForUser(booking, eventType));
        } else {
            eventPublisher.publishEvent(bookerEventForGuest(booking, eventType));
        }
    }

    private NotificationRequestedEvent bookerEventForUser(Booking booking, NotificationEventType eventType) {
        if (eventType == NotificationEventType.BOOKING_RESCHEDULED) {
            return NotificationRequestedEvent.forUser(booking.getUserId(), eventType);
        }
        return NotificationRequestedEvent.forUser(booking.getUserId(), eventType, "BOOKING", booking.getId());
    }

    private NotificationRequestedEvent bookerEventForGuest(Booking booking, NotificationEventType eventType) {
        if (eventType == NotificationEventType.BOOKING_RESCHEDULED) {
            return NotificationRequestedEvent.forGuestWithContact(
                    booking.getGuest().getId(),
                    guestPhoneProtector.decrypt(booking.getGuest()),
                    booking.getGuest().getName(),
                    eventType);
        }
        return NotificationRequestedEvent.forGuestWithContact(
                booking.getGuest().getId(),
                guestPhoneProtector.decrypt(booking.getGuest()),
                booking.getGuest().getName(),
                eventType,
                "BOOKING",
                booking.getId());
    }
}
