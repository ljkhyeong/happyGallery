package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] §10.2 예약 리마인드 배치 검증.
 *
 * <p>Proof (§10.2 DoD): D-1 / 당일 리마인드가 올바른 예약에만 발송됨.
 */
@UseCaseIT
class BookingReminderBatchUseCaseIT {

    @Autowired BookingReminderBatchUseCase bookingReminderBatchService;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @BeforeEach
    void setUp() {
        cleanup();
    }

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

    @DisplayName("D-1 리마인드 배치는 내일 슬롯에 알림을 발송한다")
    @Test
    void sendD1Reminders_tomorrowSlot_sendsNotification() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime slotStart = tomorrow.atTime(10, 0);

        Booking booking = createBooking(slotStart);

        BatchResult result = bookingReminderBatchService.sendD1Reminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(logs).hasSize(1);
            if (!logs.isEmpty()) {
                softly.assertThat(logs.get(0).getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
                softly.assertThat(logs.get(0).getGuestId()).isEqualTo(booking.getGuest().getId());
            }
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

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(logs).hasSize(1);
            if (!logs.isEmpty()) {
                softly.assertThat(logs.get(0).getEventType()).isEqualTo(NotificationEventType.REMINDER_SAME_DAY);
                softly.assertThat(logs.get(0).getGuestId()).isEqualTo(booking.getGuest().getId());
            }
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

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(logs).hasSize(1);
            if (!logs.isEmpty()) {
                softly.assertThat(logs.get(0).getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
                softly.assertThat(logs.get(0).getUserId()).isEqualTo(booking.getUserId());
                softly.assertThat(logs.get(0).getGuestId()).isNull();
            }
        });
    }

    @DisplayName("당일 리마인드 배치는 회원과 게스트 예약 모두에 알림을 발송한다")
    @Test
    void sendSameDayReminders_mixedBookings_sendsAll() {
        LocalDateTime slotStart = LocalDate.now(clock).atTime(14, 0);

        createBooking(slotStart);
        BookingClass cls2 = classStorePort.save(
                bookingClass("혼합 클래스", "MIX", 60, 30_000L, 30));
        Slot slot2 = slotStorePort.save(slot(cls2, slotStart.plusHours(1), slotStart.plusHours(2)));
        User user = userStorePort.save(new User("mixed@test.com", "hash", "혼합회원", "01088887777"));
        bookingStorePort.save(Booking.forMemberDeposit(user.getId(), slot2, 10_000L, 20_000L, DepositPaymentMethod.CARD));

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 2);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(2);
            softly.assertThat(logs).hasSize(2);
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
                Booking.forMemberDeposit(user.getId(), slot, 10_000L, 20_000L, DepositPaymentMethod.CARD));
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
                UUID.randomUUID().toString().replace("-", "")));
        return booking;
    }
}
