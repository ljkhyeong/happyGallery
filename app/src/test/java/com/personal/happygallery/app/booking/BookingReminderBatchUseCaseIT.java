package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
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

import static com.personal.happygallery.support.TestDataCleaner.clearBookingReminderData;
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

    @Autowired BookingReminderBatchService bookingReminderBatchService;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired ClassRepository classRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired NotificationLogRepository notificationLogRepository;
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
        clearBookingReminderData(
                passLedgerRepository,
                passPurchaseRepository,
                bookingHistoryRepository,
                bookingRepository,
                guestRepository,
                slotRepository,
                classRepository,
                notificationLogRepository);
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
        List<NotificationLog> logs = notificationLogRepository.findAll();

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

        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isZero();
        assertThat(notificationLogRepository.findAll()).isEmpty();
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
        List<NotificationLog> logs = notificationLogRepository.findAll();

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

        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isZero();
        assertThat(notificationLogRepository.findAll()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------

    private Booking createBooking(LocalDateTime slotStart) {
        BookingClass cls = classRepository.save(
                bookingClass("테스트 클래스", "TEST", 60, 30_000L, 30));
        Slot slot = slotRepository.save(
                slot(cls, slotStart, slotStart.plusHours(1)));
        Guest guest = guestRepository.save(guest("홍길동", "01099998888"));
        Booking booking = bookingRepository.save(
                new Booking(guest, slot, 10_000L, 20_000L,
                        DepositPaymentMethod.CARD, UUID.randomUUID().toString().replace("-", "")));
        return booking;
    }
}
