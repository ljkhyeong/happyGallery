package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.web.admin.dto.AdminBookingResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.infra.user.UserRepository;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestDataCleaner.clearBookingReminderData;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] B1-T5: admin booking list 와 reminder batch 에서
 * guest/member/claimed booking 이 모두 누락되지 않는지 검증.
 */
@UseCaseIT
class AdminBookingQueryUseCaseIT {

    @Autowired AdminBookingQueryService adminBookingQueryService;
    @Autowired BookingReminderBatchService bookingReminderBatchService;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired ClassRepository classRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired NotificationLogRepository notificationLogRepository;
    @Autowired UserRepository userRepository;
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
                passLedgerRepository, passPurchaseRepository,
                bookingHistoryRepository, bookingRepository,
                guestRepository, slotRepository,
                classRepository, notificationLogRepository);
        userRepository.deleteAllInBatch();
    }

    // ── admin booking list ───────────────────────────────────

    @DisplayName("관리자 예약 목록에 guest, member, claimed 예약이 모두 포함된다")
    @Test
    void listBookings_includesGuestMemberAndClaimedBookings() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime slotStart = tomorrow.atTime(10, 0);

        // guest booking
        BookingClass cls1 = classRepository.save(bookingClass("게스트 클래스", "G1", 60, 30_000L, 30));
        Slot slot1 = slotRepository.save(slot(cls1, slotStart, slotStart.plusHours(1)));
        Guest g = guestRepository.save(guest("게스트", "01011112222"));
        bookingRepository.save(booking(g, slot1, 10_000L, 20_000L, DepositPaymentMethod.CARD,
                UUID.randomUUID().toString().replace("-", "")));

        // member booking
        BookingClass cls2 = classRepository.save(bookingClass("회원 클래스", "M1", 60, 30_000L, 30));
        Slot slot2 = slotRepository.save(slot(cls2, slotStart.plusHours(1), slotStart.plusHours(2)));
        User member = userRepository.save(new User("member@test.com", "hash", "회원", "01033334444"));
        bookingRepository.save(Booking.forMemberDeposit(member.getId(), slot2, 10_000L, 20_000L, DepositPaymentMethod.CARD));

        // claimed booking (originally guest → claimed to user)
        BookingClass cls3 = classRepository.save(bookingClass("클레임 클래스", "C1", 60, 30_000L, 30));
        Slot slot3 = slotRepository.save(slot(cls3, slotStart.plusHours(2), slotStart.plusHours(3)));
        Guest g2 = guestRepository.save(guest("클레임대상", "01055556666"));
        User claimer = userRepository.save(new User("claimer@test.com", "hash", "클레이머", "01055556666"));
        Booking claimed = bookingRepository.save(booking(g2, slot3, 10_000L, 20_000L, DepositPaymentMethod.CARD,
                UUID.randomUUID().toString().replace("-", "")));
        claimed.claimToUser(claimer.getId());
        bookingRepository.save(claimed);

        List<AdminBookingResponse> responses = adminBookingQueryService.listBookings(tomorrow, null);

        assertSoftly(softly -> {
            softly.assertThat(responses).hasSize(3);

            List<String> types = responses.stream().map(AdminBookingResponse::bookerType).toList();
            softly.assertThat(types).contains("GUEST", "MEMBER");

            // claimed booking 은 userId 가 있으므로 MEMBER 로 표시된다
            long memberCount = types.stream().filter("MEMBER"::equals).count();
            softly.assertThat(memberCount).as("member + claimed 합계").isEqualTo(2);
        });
    }

    // ── reminder batch: claimed booking ──────────────────────

    @DisplayName("D-1 리마인드 배치는 claimed 예약에도 알림을 발송한다")
    @Test
    void sendD1Reminders_claimedBooking_sendsNotification() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime slotStart = tomorrow.atTime(10, 0);

        BookingClass cls = classRepository.save(bookingClass("클레임리마인드", "CR", 60, 30_000L, 30));
        Slot slot = slotRepository.save(slot(cls, slotStart, slotStart.plusHours(1)));
        Guest g = guestRepository.save(guest("원래게스트", "01077778888"));
        User claimer = userRepository.save(new User("remind@test.com", "hash", "리마인드회원", "01077778888"));
        Booking claimed = bookingRepository.save(booking(g, slot, 10_000L, 20_000L, DepositPaymentMethod.CARD,
                UUID.randomUUID().toString().replace("-", "")));
        claimed.claimToUser(claimer.getId());
        bookingRepository.save(claimed);

        BatchResult result = bookingReminderBatchService.sendD1Reminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogRepository, 1);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(logs).hasSize(1);
            if (!logs.isEmpty()) {
                softly.assertThat(logs.get(0).getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
                softly.assertThat(logs.get(0).getUserId()).isEqualTo(claimer.getId());
                softly.assertThat(logs.get(0).getGuestId()).isNull();
            }
        });
    }

    @DisplayName("당일 리마인드 배치는 guest, member, claimed 예약 모두에 알림을 발송한다")
    @Test
    void sendSameDayReminders_allBookingTypes_sendsAll() {
        LocalDateTime slotStart = LocalDate.now(clock).atTime(14, 0);

        // guest
        BookingClass cls1 = classRepository.save(bookingClass("게스트", "G2", 60, 30_000L, 30));
        Slot slot1 = slotRepository.save(slot(cls1, slotStart, slotStart.plusHours(1)));
        Guest g1 = guestRepository.save(guest("게스트1", "01011111111"));
        bookingRepository.save(booking(g1, slot1, 10_000L, 20_000L, DepositPaymentMethod.CARD,
                UUID.randomUUID().toString().replace("-", "")));

        // member
        BookingClass cls2 = classRepository.save(bookingClass("회원", "M2", 60, 30_000L, 30));
        Slot slot2 = slotRepository.save(slot(cls2, slotStart.plusHours(1), slotStart.plusHours(2)));
        User user = userRepository.save(new User("m@test.com", "hash", "회원1", "01022222222"));
        bookingRepository.save(Booking.forMemberDeposit(user.getId(), slot2, 10_000L, 20_000L, DepositPaymentMethod.CARD));

        // claimed
        BookingClass cls3 = classRepository.save(bookingClass("클레임", "C2", 60, 30_000L, 30));
        Slot slot3 = slotRepository.save(slot(cls3, slotStart.plusHours(2), slotStart.plusHours(3)));
        Guest g2 = guestRepository.save(guest("원래게스트2", "01033333333"));
        User claimer = userRepository.save(new User("c@test.com", "hash", "클레이머2", "01033333333"));
        Booking claimed = bookingRepository.save(booking(g2, slot3, 10_000L, 20_000L, DepositPaymentMethod.CARD,
                UUID.randomUUID().toString().replace("-", "")));
        claimed.claimToUser(claimer.getId());
        bookingRepository.save(claimed);

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogRepository, 3);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(3);
            softly.assertThat(logs).hasSize(3);
        });
    }
}
