package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.notification.NotificationOutboxService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 예약 리마인드 배치 서비스 (§10.2).
 *
 * <p>D-1(전날 자정)과 당일(당일 07:00) 두 번 발송한다.
 * guest booking 은 notifyByGuestId, member booking 은 notifyByUserId 로 분기한다.
 */
@Service
public class DefaultBookingReminderBatchService implements BookingReminderBatchUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultBookingReminderBatchService.class);
    private static final LocalTime SAME_DAY_REMINDER_START = LocalTime.of(7, 0);

    private final BookingReaderPort bookingReaderPort;
    private final NotificationOutboxService notificationOutboxService;
    private final Clock clock;

    public DefaultBookingReminderBatchService(BookingReaderPort bookingReaderPort,
                                              NotificationOutboxService notificationOutboxService,
                                              Clock clock) {
        this.bookingReaderPort = bookingReaderPort;
        this.notificationOutboxService = notificationOutboxService;
        this.clock = clock;
    }

    /**
     * D-1 리마인드 — 내일 시작하는 BOOKED 예약 대상.
     *
     * @return 발송 건수
     */
    @Override
    public BatchResult sendD1Reminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        LocalDateTime start = now.toLocalTime().isBefore(SAME_DAY_REMINDER_START)
                ? now
                : tomorrow.atStartOfDay();
        LocalDateTime end = tomorrow.plusDays(1).atStartOfDay();

        List<Booking> bookings = bookingReaderPort.findBookedInRange(start, end);
        return BatchExecutor.execute(bookings, Booking::getId,
                booking -> requestReminder(booking, NotificationEventType.REMINDER_D1),
                "D-1 예약 리마인드");
    }

    /**
     * 당일 리마인드 — 오늘 시작하는 BOOKED 예약 대상.
     *
     * @return 발송 건수
     */
    @Override
    public BatchResult sendSameDayReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.toLocalTime().isBefore(SAME_DAY_REMINDER_START)) {
            return BatchResult.successOnly(0);
        }
        LocalDateTime end = now.toLocalDate().plusDays(1).atStartOfDay();

        List<Booking> bookings = bookingReaderPort.findBookedInRange(now, end);
        return BatchExecutor.execute(bookings, Booking::getId,
                booking -> requestReminder(booking, NotificationEventType.REMINDER_SAME_DAY),
                "당일 예약 리마인드");
    }

    private boolean requestReminder(Booking booking, NotificationEventType eventType) {
        NotificationRequestedEvent event;
        if (booking.getUserId() != null) {
            event = NotificationRequestedEvent.forUser(
                    booking.getUserId(), eventType, "BOOKING", booking.getId());
        } else {
            event = NotificationRequestedEvent.forGuest(
                    booking.getGuest().getId(), eventType, "BOOKING", booking.getId());
        }
        boolean enqueued = notificationOutboxService.enqueue(event);
        if (enqueued) {
            log.info("리마인드 요청 [bookingId={}, type={}]", booking.getId(), eventType);
        }
        return enqueued;
    }
}
