package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReminderCandidatePort;
import com.personal.happygallery.application.booking.port.out.BookingReminderTarget;
import com.personal.happygallery.application.notification.NotificationOutboxService;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private static final int PAGE_SIZE = 100;

    private final BookingReminderCandidatePort reminderCandidatePort;
    private final NotificationOutboxService notificationOutboxService;
    private final Clock clock;

    public DefaultBookingReminderBatchService(
            BookingReminderCandidatePort reminderCandidatePort,
            NotificationOutboxService notificationOutboxService,
            Clock clock) {
        this.reminderCandidatePort = reminderCandidatePort;
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

        return sendReminders(
                start, end, NotificationEventType.REMINDER_D1, "D-1 예약 리마인드");
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

        return sendReminders(
                now, end, NotificationEventType.REMINDER_SAME_DAY, "당일 예약 리마인드");
    }

    private BatchResult sendReminders(
            LocalDateTime start,
            LocalDateTime end,
            NotificationEventType eventType,
            String label) {
        return BatchExecutor.executeByIdCursor(
                afterId -> reminderCandidatePort.findUnnotifiedBookedAfterId(
                        start, end, eventType, afterId, PAGE_SIZE),
                BookingReminderTarget::bookingId,
                target -> requestReminder(target, eventType),
                label);
    }

    private boolean requestReminder(
            BookingReminderTarget target, NotificationEventType eventType) {
        NotificationRequestedEvent event = target.userId() != null
                ? NotificationRequestedEvent.forUserOncePerAggregate(
                        target.userId(), eventType, "BOOKING", target.bookingId())
                : NotificationRequestedEvent.forGuestOncePerAggregate(
                        target.guestId(), eventType, "BOOKING", target.bookingId());
        boolean enqueued = notificationOutboxService.enqueue(event);
        if (enqueued) {
            log.info("리마인드 요청 [bookingId={}, type={}]", target.bookingId(), eventType);
        }
        return enqueued;
    }
}
