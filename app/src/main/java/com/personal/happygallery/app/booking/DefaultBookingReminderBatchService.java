package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final BookingReaderPort bookingReaderPort;
    private final NotificationService notificationService;
    private final Clock clock;

    public DefaultBookingReminderBatchService(BookingReaderPort bookingReaderPort,
                                       NotificationService notificationService,
                                       Clock clock) {
        this.bookingReaderPort = bookingReaderPort;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * D-1 리마인드 — 내일 시작하는 BOOKED 예약 대상.
     *
     * @return 발송 건수
     */
    public BatchResult sendD1Reminders() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime start = tomorrow.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Booking> bookings = bookingReaderPort.findBookingsInRange(BookingStatus.BOOKED, start, end);
        for (Booking booking : bookings) {
            sendReminder(booking, NotificationEventType.REMINDER_D1);
        }

        return BatchResult.successOnly(bookings.size());
    }

    /**
     * 당일 리마인드 — 오늘 시작하는 BOOKED 예약 대상.
     *
     * @return 발송 건수
     */
    public BatchResult sendSameDayReminders() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Booking> bookings = bookingReaderPort.findBookingsInRange(BookingStatus.BOOKED, start, end);
        for (Booking booking : bookings) {
            sendReminder(booking, NotificationEventType.REMINDER_SAME_DAY);
        }

        return BatchResult.successOnly(bookings.size());
    }

    private void sendReminder(Booking booking, NotificationEventType eventType) {
        if (booking.getUserId() != null) {
            notificationService.notifyByUserId(booking.getUserId(), eventType);
            log.info("리마인드 발송 [bookingId={}, userId={}, type={}]",
                    booking.getId(), booking.getUserId(), eventType);
        } else if (booking.getGuest() != null) {
            notificationService.notifyByGuestId(booking.getGuest().getId(), eventType);
            log.info("리마인드 발송 [bookingId={}, guestId={}, type={}]",
                    booking.getId(), booking.getGuest().getId(), eventType);
        } else {
            log.warn("리마인드 발송 불가 — guest/userId 모두 없음 [bookingId={}]", booking.getId());
        }
    }
}
