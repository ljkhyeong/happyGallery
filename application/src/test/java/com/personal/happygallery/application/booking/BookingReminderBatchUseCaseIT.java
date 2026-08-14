package com.personal.happygallery.application.booking;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.NotificationOutboxDispatcher;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.accessToken;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

/**
 * [UseCaseIT] §10.2 예약 리마인드 배치 검증.
 *
 * <p>Proof (§10.2 DoD): D-1 / 당일 리마인드가 올바른 예약에만 발송됨.
 */
@UseCaseIT
class BookingReminderBatchUseCaseIT {

    @Autowired BookingReminderBatchUseCase bookingReminderBatchService;
    @Autowired NotificationOutboxDispatcher notificationOutboxDispatcher;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearBookingReminderData();
        cleanupSupport.clearUsers();
    }

    // -----------------------------------------------------------------------
    // D-1 리마인드: 내일 슬롯 예약 → 알림 발송
    // -----------------------------------------------------------------------

    @DisplayName("D-1 리마인드는 비회원 예약이 회원에게 귀속돼도 한 번만 발송한다")
    @Test
    void sendD1Reminders_tomorrowSlot_sendsNotification() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime slotStart = tomorrow.atTime(10, 0);

        Booking booking = createBooking(slotStart);
        Long guestId = booking.getGuest().getId();

        BatchResult result = bookingReminderBatchService.sendD1Reminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);
        User user = userStorePort.save(
                new User("claimed-reminder@test.com", "hash", "귀속회원", "01012341234"));
        booking.claimToUser(user.getId());
        bookingStorePort.save(booking);
        BatchResult repeated = bookingReminderBatchService.sendD1Reminders();
        awaitLogCount(notificationLogProbe, 1);
        NotificationLog log = logs.getFirst();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(repeated.successCount()).isZero();
            softly.assertThat(repeated.failureCount()).isZero();
            softly.assertThat(log.getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
            softly.assertThat(log.getGuestId()).isEqualTo(guestId);
        });
    }

    @DisplayName("구형 수신자 기반 outbox가 있으면 D-1 리마인드를 다시 요청하지 않는다")
    @Test
    void sendD1Reminders_legacyRecipientKeyExists_skipsAggregate() {
        Booking booking = createBooking(
                LocalDate.now(clock).plusDays(1).atTime(10, 0));
        Long bookingId = booking.getId();
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationOutbox legacyOutbox = NotificationOutbox.from(
                NotificationRequestedEvent.forGuest(
                        booking.getGuest().getId(),
                        NotificationEventType.REMINDER_D1,
                        "BOOKING",
                        bookingId),
                now);
        String processingToken = legacyOutbox.markProcessing(now);
        legacyOutbox.markSent(processingToken, now);
        notificationOutboxRepository.saveAndFlush(legacyOutbox);

        BatchResult result = bookingReminderBatchService.sendD1Reminders();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(notificationOutboxRepository.findAll())
                    .filteredOn(outbox ->
                            outbox.getEventType() == NotificationEventType.REMINDER_D1
                                    && bookingId.equals(outbox.getAggregateId()))
                    .singleElement()
                    .satisfies(outbox -> softly.assertThat(outbox.getIdempotencyKey())
                            .startsWith("GUEST:"));
            softly.assertThat(notificationLogProbe.all()).isEmpty();
        });
    }

    // -----------------------------------------------------------------------
    // D-1 리마인드: 오늘 슬롯 예약 → 스킵
    // -----------------------------------------------------------------------

    @DisplayName("D-1 리마인드 배치는 당일 슬롯을 건너뛴다")
    @Test
    void sendD1Reminders_todaySlot_skips() {
        LocalDateTime slotStart = LocalDate.now(clock).atTime(10, 0);

        createBooking(slotStart);

        BatchResult result = bookingReminderBatchService.sendD1Reminders();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(0);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(notificationLogProbe.all()).isEmpty();
        });
    }

    @DisplayName("D-1 리마인드는 내일 시작 경계와 정확히 같은 예약을 포함한다")
    @Test
    void sendD1Reminders_slotAtTomorrowStart_sendsNotification() {
        LocalDateTime tomorrowStart = LocalDate.now(clock).plusDays(1).atStartOfDay();

        createBooking(tomorrowStart);

        BatchResult result = bookingReminderBatchService.sendD1Reminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(logs).singleElement()
                    .extracting(NotificationLog::getEventType)
                    .isEqualTo(NotificationEventType.REMINDER_D1);
        });
    }

    // -----------------------------------------------------------------------
    // 당일 리마인드: 오늘 슬롯 예약 → 알림 발송
    // -----------------------------------------------------------------------

    @DisplayName("당일 리마인드 배치는 당일 슬롯에 알림을 발송한다")
    @Test
    void sendSameDayReminders_todaySlot_sendsNotification() {
        LocalDateTime slotStart = LocalDate.now(clock).atTime(14, 0);

        Booking booking = createBooking(slotStart);

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);
        NotificationLog log = logs.getFirst();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(log.getEventType()).isEqualTo(NotificationEventType.REMINDER_SAME_DAY);
            softly.assertThat(log.getGuestId()).isEqualTo(booking.getGuest().getId());
        });
    }

    @DisplayName("당일 리마인드는 현재 시각과 정확히 같은 이미 시작한 예약을 제외한다")
    @Test
    void sendSameDayReminders_slotAtNow_skipsNotification() {
        createBooking(LocalDateTime.now(clock));

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(notificationLogProbe.all()).isEmpty();
            softly.assertThat(notificationOutboxRepository.findAll()).isEmpty();
        });
    }

    // -----------------------------------------------------------------------
    // 당일 리마인드: 내일 슬롯 예약 → 스킵
    // -----------------------------------------------------------------------

    @DisplayName("당일 리마인드 배치는 내일 슬롯을 건너뛴다")
    @Test
    void sendSameDayReminders_tomorrowSlot_skips() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime slotStart = tomorrow.atTime(10, 0);

        createBooking(slotStart);

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(0);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(notificationLogProbe.all()).isEmpty();
        });
    }

    // -----------------------------------------------------------------------
    // Q1-T5: member booking 리마인드 — userId 기반 알림 발송
    // -----------------------------------------------------------------------

    @DisplayName("D-1 리마인드 배치는 회원 예약에도 알림을 발송한다")
    @Test
    void sendD1Reminders_memberBooking_sendsNotification() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime slotStart = tomorrow.atTime(10, 0);

        Booking booking = createMemberBooking(slotStart);

        BatchResult result = bookingReminderBatchService.sendD1Reminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);
        NotificationLog log = logs.getFirst();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(log.getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
            softly.assertThat(log.getUserId()).isEqualTo(booking.getUserId());
            softly.assertThat(log.getGuestId()).isNull();
        });
    }

    @DisplayName("당일 리마인드 배치는 회원과 게스트 예약 모두에 알림을 발송한다")
    @Test
    void sendSameDayReminders_mixedBookings_sendsAll() {
        LocalDateTime slotStart = LocalDate.now(clock).atTime(14, 0);

        Booking guestBooking = createBooking(slotStart);
        BookingClass cls2 = classStorePort.save(
                bookingClass("혼합 클래스", "MIX", 60, 30_000L, 30));
        Slot slot2 = slotStorePort.save(slot(cls2, slotStart.plusHours(1), slotStart.plusHours(2)));
        User user = userStorePort.save(new User("mixed@test.com", "hash", "혼합회원", "01088887777"));
        Booking memberBooking = bookingStorePort.save(
                Booking.forMemberDeposit(
                        user, slot2, 10_000L, 20_000L, DepositPaymentMethod.CARD));

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 2);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(2);
            softly.assertThat(logs)
                    .extracting(NotificationLog::getGuestId, NotificationLog::getUserId)
                    .containsExactlyInAnyOrder(
                            tuple(guestBooking.getGuest().getId(), null),
                            tuple(null, memberBooking.getUserId()));
        });
    }

    @DisplayName("발송 전에 취소된 예약 리마인드는 외부 발송 없이 종결한다")
    @Test
    void dispatchReminder_afterBookingCancellation_marksObsolete() {
        LocalDateTime now = LocalDateTime.now(clock);
        Booking booking = createBooking(now.toLocalDate().plusDays(1).atTime(10, 0));
        NotificationOutbox outbox = notificationOutboxRepository.save(NotificationOutbox.from(
                NotificationRequestedEvent.forGuestOncePerAggregate(
                        booking.getGuest().getId(),
                        NotificationEventType.REMINDER_D1,
                        "BOOKING",
                        booking.getId()),
                now));
        booking.cancel();
        bookingStorePort.save(booking);

        BatchResult result = notificationOutboxDispatcher.dispatchPending();

        NotificationOutbox obsolete = notificationOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(obsolete.getStatus()).isEqualTo(NotificationOutboxStatus.OBSOLETE);
            softly.assertThat(obsolete.getLastError()).isEqualTo("REMINDER_NO_LONGER_ELIGIBLE");
            softly.assertThat(notificationLogProbe.all()).isEmpty();
        });
    }

    @DisplayName("비회원 예약 리마인드는 발송 준비 전에 회원 귀속되면 현재 회원에게 발송한다")
    @Test
    void dispatchReminder_afterBookingClaim_refreshesRecipientBeforeDelivery() {
        LocalDateTime now = LocalDateTime.now(clock);
        Booking booking = createBooking(now.toLocalDate().plusDays(1).atTime(10, 0));
        Long previousGuestId = booking.getGuest().getId();
        NotificationOutbox outbox = notificationOutboxRepository.saveAndFlush(NotificationOutbox.from(
                NotificationRequestedEvent.forGuestOncePerAggregate(
                        previousGuestId,
                        NotificationEventType.REMINDER_D1,
                        "BOOKING",
                        booking.getId()),
                now));
        User currentOwner = userStorePort.save(
                new User("claimed-before-dispatch@test.com", "hash", "현재회원", "01012344321"));
        booking.claimToUser(currentOwner.getId());
        bookingStorePort.save(booking);

        BatchResult result = notificationOutboxDispatcher.dispatchPending();
        NotificationLog log = awaitLogCount(notificationLogProbe, 1).getFirst();
        NotificationOutbox sent = notificationOutboxRepository.findById(outbox.getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(log.getUserId()).isEqualTo(currentOwner.getId());
            softly.assertThat(log.getGuestId()).isNull();
            softly.assertThat(sent.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
            softly.assertThat(sent.getUserId()).isEqualTo(currentOwner.getId());
            softly.assertThat(sent.getGuestId()).isNull();
            softly.assertThat(sent.getRecipientType().name()).isEqualTo("USER");
        });
    }

    @DisplayName("다시 유효해진 D-1 예약은 기존 OBSOLETE 행과 멱등키로 알림을 재개한다")
    @Test
    void sendD1Reminders_whenObsoleteReminderBecomesEligible_reactivatesSameOutbox() {
        LocalDateTime now = LocalDateTime.now(clock);
        Booking booking = createBooking(now.toLocalDate().plusDays(1).atTime(10, 0));
        NotificationOutbox outbox = NotificationOutbox.from(
                NotificationRequestedEvent.forGuestOncePerAggregate(
                        booking.getGuest().getId(),
                        NotificationEventType.REMINDER_D1,
                        "BOOKING",
                        booking.getId()),
                now.minusHours(1));
        String token = outbox.markProcessing(now.minusHours(1));
        outbox.markObsolete(token, now.minusMinutes(30), "REMINDER_NO_LONGER_ELIGIBLE");
        NotificationOutbox savedOutbox = notificationOutboxRepository.saveAndFlush(outbox);
        Long outboxId = savedOutbox.getId();
        String idempotencyKey = savedOutbox.getIdempotencyKey();

        BatchResult result = bookingReminderBatchService.sendD1Reminders();
        awaitLogCount(notificationLogProbe, 1);

        await().atMost(2, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(notificationOutboxRepository.findById(outboxId))
                        .hasValueSatisfying(saved -> assertThat(saved.getStatus())
                                .isEqualTo(NotificationOutboxStatus.SENT)));
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(notificationOutboxRepository.findAll())
                    .singleElement()
                    .satisfies(saved -> {
                        softly.assertThat(saved.getId()).isEqualTo(outboxId);
                        softly.assertThat(saved.getIdempotencyKey())
                                .isEqualTo(idempotencyKey);
                    });
        });
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------

    private Booking createMemberBooking(LocalDateTime slotStart) {
        BookingClass cls = classStorePort.save(
                bookingClass("회원 클래스", "MEMBER", 60, 30_000L, 30));
        Slot slot = slotStorePort.save(slot(cls, slotStart, slotStart.plusHours(1)));
        User user = userStorePort.save(new User("reminder@test.com", "hash", "회원테스트", "01077776666"));
        Booking booking = bookingStorePort.save(
                Booking.forMemberDeposit(
                        user, slot, 10_000L, 20_000L, DepositPaymentMethod.CARD));
        return booking;
    }

    private Booking createBooking(LocalDateTime slotStart) {
        BookingClass cls = classStorePort.save(
                bookingClass("테스트 클래스", "TEST", 60, 30_000L, 30));
        Slot slot = slotStorePort.save(
                slot(cls, slotStart, slotStart.plusHours(1)));
        Guest guest = guestStorePort.save(guest("홍길동", "01099998888"));
        Booking booking = bookingStorePort.save(booking(
                guest,
                slot,
                10_000L,
                20_000L,
                DepositPaymentMethod.CARD,
                accessToken()));
        return booking;
    }
}
