package com.personal.happygallery.application.booking;

import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.booking.port.in.BookingSettlementUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.TestFixtures.accessToken;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class BookingSettlementUseCaseIT {

    private static final long ADMIN_ID = 7L;

    @Autowired BookingSettlementUseCase settlementUseCase;
    @Autowired BookingReaderPort bookingReaderPort;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotReaderPort slotReaderPort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("미수와 잔금 상태가 실제 변경될 때만 관리자 정산 이력이 순서대로 기록된다")
    @Test
    void settleArrears_completeAndPayBalance() {
        BookingClass bookingClass = classStorePort.save(defaultBookingClass());
        Slot slot = slotStorePort.save(slot(
                bookingClass,
                LocalDateTime.now(clock).minusHours(3),
                LocalDateTime.now(clock).minusHours(1)));
        slot.incrementBookedCount();
        slotStorePort.save(slot);
        Guest guest = guestStorePort.save(guest("미수 고객", "01011112222"));
        Booking booking = bookingStorePort.save(booking(
                guest, slot, 5_000L, 45_000L, DepositPaymentMethod.CARD, accessToken()));

        assertThatThrownBy(() -> settlementUseCase.complete(booking.getId(), ADMIN_ID))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("미수로 표시");

        settlementUseCase.updateArrears(booking.getId(), true, ADMIN_ID);
        settlementUseCase.updateArrears(booking.getId(), true, ADMIN_ID);
        settlementUseCase.updateArrears(booking.getId(), false, ADMIN_ID);
        settlementUseCase.updateArrears(booking.getId(), true, ADMIN_ID);
        settlementUseCase.complete(booking.getId(), ADMIN_ID);
        settlementUseCase.markBalancePaid(booking.getId(), ADMIN_ID);
        settlementUseCase.markBalancePaid(booking.getId(), ADMIN_ID);
        settlementUseCase.updateArrears(booking.getId(), false, ADMIN_ID);

        Booking settled = bookingReaderPort.findById(booking.getId()).orElseThrow();
        Slot occupiedSlot = slotReaderPort.findById(slot.getId()).orElseThrow();
        List<BookingHistory> histories = bookingHistoryRepository.findAll().stream()
                .sorted(comparing(BookingHistory::getId))
                .toList();
        assertSoftly(softly -> {
            softly.assertThat(settled.getStatus()).isEqualTo(BookingStatus.COMPLETED);
            softly.assertThat(settled.getBalanceStatus()).isEqualTo(BalanceStatus.PAID);
            softly.assertThat(settled.getBalancePaidAt()).isEqualTo(LocalDateTime.now(clock));
            softly.assertThat(settled.isArrearsFlag()).isFalse();
            softly.assertThat(occupiedSlot.getBookedCount()).isEqualTo(1);
            softly.assertThat(histories)
                    .extracting(BookingHistory::getAction)
                    .containsExactly(
                            BookingHistoryAction.ARREARS_MARKED,
                            BookingHistoryAction.ARREARS_CLEARED,
                            BookingHistoryAction.ARREARS_MARKED,
                            BookingHistoryAction.COMPLETED,
                            BookingHistoryAction.BALANCE_PAID,
                            BookingHistoryAction.ARREARS_CLEARED);
            softly.assertThat(histories)
                    .allSatisfy(history -> softly.assertThat(history.getActor()).isEqualTo("ADMIN"));
            softly.assertThat(histories)
                    .allSatisfy(history -> softly.assertThat(history.getAdminUserId()).isEqualTo(ADMIN_ID));
        });
    }

    @DisplayName("회원 예약을 완료하면 예약당 한 번의 후기 작성 요청을 접수한다")
    @Test
    void complete_memberBooking_enqueuesReviewRequestOncePerBooking() {
        User user = userStorePort.save(new User(
                "booking-review@example.com", "hash", "후기 회원", "01076543210"));
        BookingClass bookingClass = classStorePort.save(defaultBookingClass());
        Slot slot = slotStorePort.save(slot(
                bookingClass,
                LocalDateTime.now(clock).minusHours(3),
                LocalDateTime.now(clock).minusHours(1)));
        slot.incrementBookedCount();
        slotStorePort.save(slot);
        Booking booking = bookingStorePort.save(Booking.forMemberDeposit(
                user, slot, 5_000L, 0L, DepositPaymentMethod.CARD));

        settlementUseCase.complete(booking.getId(), ADMIN_ID);

        assertSoftly(softly -> {
            softly.assertThat(bookingReaderPort.findById(booking.getId()).orElseThrow().getStatus())
                    .isEqualTo(BookingStatus.COMPLETED);
            softly.assertThat(notificationOutboxRepository.findAll())
                    .filteredOn(outbox -> outbox.getEventType() == NotificationEventType.REVIEW_REQUEST)
                    .singleElement()
                    .satisfies(outbox -> {
                        softly.assertThat(outbox.getAggregateType()).isEqualTo("BOOKING");
                        softly.assertThat(outbox.getAggregateId()).isEqualTo(booking.getId());
                        softly.assertThat(outbox.getIdempotencyKey())
                                .isEqualTo("AGGREGATE:REVIEW_REQUEST:BOOKING:" + booking.getId());
                    });
        });
    }
}
