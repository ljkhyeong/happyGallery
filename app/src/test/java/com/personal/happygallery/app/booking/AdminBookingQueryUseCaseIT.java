package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.app.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.app.booking.port.out.ClassStorePort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.app.customer.port.out.GuestStorePort;
import com.personal.happygallery.app.customer.port.out.UserStorePort;
import com.personal.happygallery.app.web.admin.dto.AdminBookingResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.accessToken;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] B1-T5: admin booking list 와 reminder batch 에서
 * guest/member/claimed booking 이 모두 누락되지 않는지 검증.
 */
@UseCaseIT
class AdminBookingQueryUseCaseIT {

    @Autowired AdminBookingQueryUseCase adminBookingQueryService;
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

    // ── admin booking list ───────────────────────────────────

    @DisplayName("관리자 예약 목록에 guest, member, claimed 예약이 모두 포함된다")
    @Test
    void listBookings_includesGuestMemberAndClaimedBookings() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        LocalDateTime slotStart = tomorrow.atTime(10, 0);

        saveGuestBooking(slotStart, "게스트 클래스", "G1", "게스트", "01011112222");
        saveMemberBooking(slotStart.plusHours(1), "회원 클래스", "M1", "member@test.com", "회원", "01033334444");
        saveClaimedBooking(slotStart.plusHours(2), "클레임 클래스", "C1",
                "클레임대상", "01055556666", "claimer@test.com", "클레이머");

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

        User claimer = saveClaimedBooking(slotStart, "클레임리마인드", "CR",
                "원래게스트", "01077778888", "remind@test.com", "리마인드회원");

        BatchResult result = bookingReminderBatchService.sendD1Reminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);

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

        saveGuestBooking(slotStart, "게스트", "G2", "게스트1", "01011111111");
        saveMemberBooking(slotStart.plusHours(1), "회원", "M2", "m@test.com", "회원1", "01022222222");
        saveClaimedBooking(slotStart.plusHours(2), "클레임", "C2",
                "원래게스트2", "01033333333", "c@test.com", "클레이머2");

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 3);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(3);
            softly.assertThat(logs).hasSize(3);
        });
    }

    // ── helpers ──────────────────────────────────────────────

    private Slot saveSlot(LocalDateTime start, String className, String category) {
        BookingClass cls = classStorePort.save(bookingClass(className, category, 60, 30_000L, 30));
        return slotStorePort.save(slot(cls, start, start.plusHours(1)));
    }

    private void saveGuestBooking(LocalDateTime slotStart, String className, String category,
                                  String guestName, String phone) {
        Slot s = saveSlot(slotStart, className, category);
        Guest g = guestStorePort.save(guest(guestName, phone));
        bookingStorePort.save(booking(g, s, 10_000L, 20_000L, DepositPaymentMethod.CARD, accessToken()));
    }

    private void saveMemberBooking(LocalDateTime slotStart, String className, String category,
                                   String email, String name, String phone) {
        Slot s = saveSlot(slotStart, className, category);
        User member = userStorePort.save(new User(email, "hash", name, phone));
        bookingStorePort.save(Booking.forMemberDeposit(member.getId(), s, 10_000L, 20_000L, DepositPaymentMethod.CARD));
    }

    private User saveClaimedBooking(LocalDateTime slotStart, String className, String category,
                                    String guestName, String phone, String claimerEmail, String claimerName) {
        Slot s = saveSlot(slotStart, className, category);
        Guest g = guestStorePort.save(guest(guestName, phone));
        User claimer = userStorePort.save(new User(claimerEmail, "hash", claimerName, phone));
        Booking claimed = bookingStorePort.save(
                booking(g, s, 10_000L, 20_000L, DepositPaymentMethod.CARD, accessToken()));
        claimed.claimToUser(claimer.getId());
        bookingStorePort.save(claimed);
        return claimer;
    }
}
