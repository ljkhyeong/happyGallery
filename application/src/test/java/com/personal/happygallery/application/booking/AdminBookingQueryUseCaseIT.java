package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.dashboard.port.in.DashboardQueryUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingStatus;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.accessToken;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] B1-T5: admin booking list 와 reminder batch 에서
 * guest/member/claimed booking 이 모두 누락되지 않는지 검증.
 */
@UseCaseIT
class AdminBookingQueryUseCaseIT {

    @Autowired AdminBookingQueryUseCase adminBookingQueryService;
    @Autowired AdminBookingSearchUseCase adminBookingSearchUseCase;
    @Autowired DashboardQueryUseCase dashboardQueryUseCase;
    @Autowired BookingReminderBatchUseCase bookingReminderBatchService;
    @Autowired CustomerAccountLifecycleUseCase customerAccountLifecycleUseCase;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired NotificationLogProbe notificationLogProbe;
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
            softly.assertThat(responses)
                    .extracting(
                            AdminBookingResponse::className,
                            AdminBookingResponse::bookerType,
                            AdminBookingResponse::bookerName,
                            AdminBookingResponse::bookerPhone)
                    .containsExactly(
                            tuple("게스트 클래스", "GUEST", "게스트", "01011112222"),
                            tuple("회원 클래스", "MEMBER", "회원", "01033334444"),
                            tuple("클레임 클래스", "MEMBER", "클레이머", "01055556666"));
        });
    }

    @DisplayName("관리자 예약 목록과 검색은 회원 예약이 없는 날의 비회원 예약도 조회한다")
    @Test
    void listBookings_guestOnly_returnsGuestBookings() {
        LocalDate targetDate = LocalDate.now(clock).plusDays(2);
        LocalDateTime slotStart = targetDate.atTime(10, 0);

        saveGuestBooking(slotStart, "게스트전용 클래스", "GUEST_ONLY", "게스트", "01012121212");
        Slot canceledSlot = saveSlot(slotStart.plusHours(1), "취소 클래스", "CANCELED_ONLY");
        Guest canceledGuest = guestStorePort.save(guest("취소자", "01034343434"));
        Booking canceled = booking(
                canceledGuest, canceledSlot, 10_000L, 20_000L,
                DepositPaymentMethod.CARD, accessToken());
        canceled.cancel();
        bookingStorePort.save(canceled);

        List<AdminBookingResponse> responses = adminBookingQueryService.listBookings(targetDate, BookingStatus.BOOKED);
        OffsetPage<AdminBookingSearchRow> searchResult =
                adminBookingSearchUseCase.search(null, targetDate, targetDate, "게스트", 0, 20);
        OffsetPage<AdminBookingSearchRow> phoneSearchResult =
                adminBookingSearchUseCase.search(null, targetDate, targetDate, "010-1212-1212", 0, 20);

        assertSoftly(softly -> {
            softly.assertThat(responses).hasSize(1);
            softly.assertThat(responses.getFirst().bookerType()).isEqualTo("GUEST");
            softly.assertThat(responses.getFirst().bookerPhone()).isEqualTo("01012121212");
            softly.assertThat(searchResult.content()).hasSize(1);
            softly.assertThat(searchResult.content().getFirst().bookerPhone()).isEqualTo("01012121212");
            softly.assertThat(phoneSearchResult.content()).hasSize(1);
            softly.assertThat(phoneSearchResult.content().getFirst().bookerPhone()).isEqualTo("01012121212");
        });
    }

    @DisplayName("종결 예약 회원이 탈퇴해도 관리자 날짜 목록은 익명화된 회원 이력을 조회한다")
    @Test
    void listBookings_withdrawnMemberHistory_returnsAnonymizedMember() {
        LocalDate targetDate = LocalDate.now(clock).minusDays(1);
        Slot memberSlot = saveSlot(
                targetDate.atTime(10, 0),
                "탈퇴 회원 과거 클래스",
                "WITHDRAWN_MEMBER_HISTORY");
        User member = userStorePort.save(
                new User("withdrawn-history@test.com", "hash", "탈퇴 전 회원", "01089898989"));
        Booking completed = Booking.forMemberDeposit(
                member, memberSlot, 10_000L, 20_000L, DepositPaymentMethod.CARD);
        completed.markBalancePaid(targetDate.atTime(12, 0));
        completed.complete(LocalDateTime.now(clock));
        completed = bookingStorePort.save(completed);

        customerAccountLifecycleUseCase.withdraw(member.getId());

        List<AdminBookingResponse> responses =
                adminBookingQueryService.listBookings(targetDate, BookingStatus.COMPLETED);

        Long completedBookingId = completed.getId();
        assertSoftly(softly -> {
            softly.assertThat(responses).hasSize(1);
            softly.assertThat(responses.getFirst().bookingId()).isEqualTo(completedBookingId);
            softly.assertThat(responses.getFirst().bookerType()).isEqualTo("MEMBER");
            softly.assertThat(responses.getFirst().bookerName()).isEqualTo("탈퇴회원");
            softly.assertThat(responses.getFirst().bookerPhone()).isNull();
        });
    }

    @DisplayName("표시 예약번호만 예약 ID 정확 일치 검색으로 처리한다")
    @Test
    void searchBookings_formattedBookingNumber_matchesExactId() {
        LocalDateTime slotStart = LocalDate.now(clock).plusDays(3).atTime(10, 0);
        Slot targetSlot = saveSlot(slotStart, "검색 대상 클래스", "SEARCH_TARGET");
        Guest targetGuest = guestStorePort.save(guest("검색대상", "01056565656"));
        Booking target = bookingStorePort.save(booking(
                targetGuest, targetSlot, 10_000L, 20_000L,
                DepositPaymentMethod.CARD, accessToken()));
        saveGuestBooking(slotStart.plusHours(1), "다른 클래스", "SEARCH_OTHER", "다른예약자", "01078787878");

        OffsetPage<AdminBookingSearchRow> exactResult = adminBookingSearchUseCase.search(
                null, null, null, "BK-%08d".formatted(target.getId()), 0, 20);
        OffsetPage<AdminBookingSearchRow> bareIdResult = adminBookingSearchUseCase.search(
                null, null, null, String.valueOf(target.getId()), 0, 20);

        assertSoftly(softly -> {
            softly.assertThat(exactResult.totalCount()).isEqualTo(1);
            softly.assertThat(exactResult.content())
                    .extracting(AdminBookingSearchRow::bookingId)
                    .containsExactly(target.getId());
            softly.assertThat(bareIdResult.content()).isEmpty();
        });
    }

    @DisplayName("슬롯 이용률은 늦은 오후 슬롯도 저장된 서울 날짜로 집계한다")
    @Test
    void slotUtilization_usesStoredSeoulDate() {
        LocalDate date = LocalDate.now(clock).plusDays(4);
        BookingClass cls = classStorePort.save(
                bookingClass("날짜 경계 클래스", "DATE_BOUNDARY", 60, 30_000L, 30));
        slotStorePort.save(slot(cls, date.atTime(15, 0), date.atTime(16, 0)));
        slotStorePort.save(slot(cls, date.atTime(23, 30), date.plusDays(1).atTime(0, 30)));

        var utilization = dashboardQueryUseCase.getSlotUtilization(date, date);

        assertSoftly(softly -> {
            softly.assertThat(utilization).hasSize(1);
            softly.assertThat(utilization.getFirst().date()).isEqualTo(date);
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
        NotificationLog log = logs.getFirst();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(log.getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
            softly.assertThat(log.getUserId()).isEqualTo(claimer.getId());
            softly.assertThat(log.getGuestId()).isNull();
        });
    }

    @DisplayName("당일 리마인드 배치는 guest, member, claimed 예약 모두에 알림을 발송한다")
    @Test
    void sendSameDayReminders_allBookingTypes_sendsAll() {
        LocalDateTime slotStart = LocalDate.now(clock).atTime(14, 0);

        Guest guest = saveGuestBooking(slotStart, "게스트", "G2", "게스트1", "01011111111");
        User member = saveMemberBooking(
                slotStart.plusHours(1), "회원", "M2", "m@test.com", "회원1", "01022222222");
        User claimer = saveClaimedBooking(slotStart.plusHours(2), "클레임", "C2",
                "원래게스트2", "01033333333", "c@test.com", "클레이머2");

        BatchResult result = bookingReminderBatchService.sendSameDayReminders();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 3);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(3);
            softly.assertThat(logs)
                    .extracting(NotificationLog::getGuestId, NotificationLog::getUserId)
                    .containsExactlyInAnyOrder(
                            tuple(guest.getId(), null),
                            tuple(null, member.getId()),
                            tuple(null, claimer.getId()));
        });
    }

    // ── helpers ──────────────────────────────────────────────

    private Slot saveSlot(LocalDateTime start, String className, String category) {
        BookingClass cls = classStorePort.save(bookingClass(className, category, 60, 30_000L, 30));
        return slotStorePort.save(slot(cls, start, start.plusHours(1)));
    }

    private Guest saveGuestBooking(LocalDateTime slotStart, String className, String category,
                                   String guestName, String phone) {
        Slot s = saveSlot(slotStart, className, category);
        Guest g = guestStorePort.save(guest(guestName, phone));
        bookingStorePort.save(booking(g, s, 10_000L, 20_000L, DepositPaymentMethod.CARD, accessToken()));
        return g;
    }

    private User saveMemberBooking(LocalDateTime slotStart, String className, String category,
                                   String email, String name, String phone) {
        Slot s = saveSlot(slotStart, className, category);
        User member = userStorePort.save(new User(email, "hash", name, phone));
        bookingStorePort.save(Booking.forMemberDeposit(
                member, s, 10_000L, 20_000L, DepositPaymentMethod.CARD));
        return member;
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
