package com.personal.happygallery.application.booking;

import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionCommand;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionResult;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase.TaskView;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase.WithdrawCommand;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.pass.port.out.PassLedgerReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingCancellationTaskStatus;
import com.personal.happygallery.domain.booking.BookingCancellationTaskType;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.BookingStateProbe;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.accessToken;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@UseCaseIT
class AdminSlotSessionCancelUseCaseIT {

    private static final Long ADMIN_ID = 7L;
    private static final String REASON = "악천후로 수업 취소";

    @Autowired AdminBookingCancelUseCase adminBookingCancelUseCase;
    @Autowired BookingCancellationTaskUseCase bookingCancellationTaskUseCase;
    @Autowired SlotManagementUseCase slotManagementUseCase;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    @Autowired UserReaderPort userReaderPort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired PassPurchaseReaderPort passPurchaseReaderPort;
    @Autowired PassLedgerReaderPort passLedgerReaderPort;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired BookingStateProbe bookingStateProbe;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @MockitoBean PaymentProvider paymentProvider;

    BookingClass bookingClass;

    @BeforeEach
    void setUp() {
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenAnswer(invocation -> RefundResult.success(
                        "FAKE-" + invocation.getArgument(2, String.class)));
        bookingClass = classStorePort.save(
                bookingClass("우드 정규 클래스", "WOOD", 120, 50_000L, 30));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @DisplayName("비활성 슬롯의 예약들을 일괄 취소하면 결제 수단별 보상과 운영 이력을 함께 남긴다")
    @Test
    void cancelSession_inactiveSlot_cancelsAllBookingsAndAggregatesCompensations() {
        Slot slot = slotStorePort.save(slot(bookingClass, FUTURE, FUTURE.plusHours(2)));
        Booking unpaidDeposit = saveGuestDepositBooking(slot, "01", false);
        Booking paidBalanceDeposit = saveGuestDepositBooking(slot, "02", true);
        PassBooking validPass = savePassBooking(
                slot, "valid", "01020000001", LocalDateTime.now(clock).plusDays(30));
        PassBooking expiredPass = savePassBooking(
                slot, "expired", "01020000002", LocalDateTime.now(clock).minusMinutes(1));
        occupy(slot, 4);
        slotManagementUseCase.deactivateSlot(slot.getId());

        CancelSessionResult result = adminBookingCancelUseCase.cancelSession(
                new CancelSessionCommand(slot.getId(), ADMIN_ID, REASON));

        List<Refund> refunds = awaitSucceededRefunds(2);
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 6);
        List<BookingHistory> histories = bookingHistoryRepository.findAll();
        List<TaskView> cancellationTasks =
                bookingCancellationTaskUseCase.listPending();
        TaskView balanceTask = cancellationTasks.stream()
                .filter(task -> task.type() == BookingCancellationTaskType.BALANCE_SETTLEMENT)
                .findFirst()
                .orElseThrow();
        var firstCompletion = bookingCancellationTaskUseCase.complete(balanceTask.taskId(), ADMIN_ID);
        var repeatedCompletion = bookingCancellationTaskUseCase.complete(balanceTask.taskId(), 99L);
        List<TaskView> remainingTasks =
                bookingCancellationTaskUseCase.listPending();
        User expiredPassOwner = userReaderPort
                .findById(expiredPass.pass().getUserId())
                .orElseThrow();
        assertThatThrownBy(() -> accountLifecycleUseCase.withdraw(new WithdrawCommand(
                expiredPassOwner.getId(),
                expiredPassOwner.getCredentialVersion(),
                true)))
                .isInstanceOfSatisfying(HappyGalleryException.class, error ->
                        assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWAL_BLOCKED));

        assertSoftly(softly -> {
            softly.assertThat(result.canceledBookings()).isEqualTo(4);
            softly.assertThat(result.passCreditsRestored()).isEqualTo(1);
            softly.assertThat(result.depositRefundsRequested()).isEqualTo(2);
            softly.assertThat(result.balanceSettlementsRequired()).isEqualTo(1);
            softly.assertThat(result.manualCompensationsRequired()).isEqualTo(1);
            softly.assertThat(cancellationTasks)
                    .extracting(TaskView::type)
                    .containsExactlyInAnyOrder(
                            BookingCancellationTaskType.BALANCE_SETTLEMENT,
                            BookingCancellationTaskType.MANUAL_COMPENSATION);
            softly.assertThat(cancellationTasks)
                    .extracting(TaskView::bookingId)
                    .containsExactlyInAnyOrder(
                            paidBalanceDeposit.getId(),
                            expiredPass.booking().getId());
            softly.assertThat(cancellationTasks)
                    .allSatisfy(task -> {
                        assertThat(task.status()).isEqualTo(BookingCancellationTaskStatus.PENDING);
                        assertThat(task.reason()).isEqualTo(REASON);
                        assertThat(task.createdAt()).isNotNull();
                    });
            softly.assertThat(cancellationTasks)
                    .filteredOn(task -> task.type() == BookingCancellationTaskType.MANUAL_COMPENSATION)
                    .singleElement()
                    .extracting(TaskView::compensationAmount)
                    .isEqualTo(0L);
            softly.assertThat(firstCompletion.changed()).isTrue();
            softly.assertThat(repeatedCompletion.changed()).isFalse();
            softly.assertThat(repeatedCompletion.task().status())
                    .isEqualTo(BookingCancellationTaskStatus.COMPLETED);
            softly.assertThat(repeatedCompletion.task().completedByAdminId()).isEqualTo(ADMIN_ID);
            softly.assertThat(repeatedCompletion.task().completedAt()).isNotNull();
            softly.assertThat(remainingTasks)
                    .extracting(TaskView::type)
                    .containsExactly(BookingCancellationTaskType.MANUAL_COMPENSATION);
            softly.assertThat(List.of(
                            unpaidDeposit.getId(),
                            paidBalanceDeposit.getId(),
                            validPass.booking().getId(),
                            expiredPass.booking().getId()))
                    .extracting(id -> bookingStateProbe.getBooking(id).getStatus())
                    .containsOnly(BookingStatus.CANCELED);
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isZero();
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).isAdminActive()).isFalse();
            softly.assertThat(refunds).extracting(Refund::getStatus).containsOnly(RefundStatus.SUCCEEDED);
            softly.assertThat(passPurchaseReaderPort.findById(validPass.pass().getId()).orElseThrow()
                            .getRemainingCredits())
                    .isEqualTo(8);
            softly.assertThat(passPurchaseReaderPort.findById(expiredPass.pass().getId()).orElseThrow()
                            .getRemainingCredits())
                    .isZero();
            softly.assertThat(passLedgerReaderPort.findByPassPurchaseId(validPass.pass().getId()))
                    .extracting(ledger -> ledger.getType())
                    .containsExactly(PassLedgerType.REFUND);
            softly.assertThat(passLedgerReaderPort.findByPassPurchaseId(expiredPass.pass().getId()))
                    .extracting(ledger -> ledger.getType())
                    .containsExactly(PassLedgerType.EXPIRE);
            softly.assertThat(histories).hasSize(4)
                    .allSatisfy(history -> {
                        assertThat(history.getAction()).isEqualTo(BookingHistoryAction.CANCELED);
                        assertThat(history.getActor()).isEqualTo("ADMIN");
                        assertThat(history.getAdminUserId()).isEqualTo(ADMIN_ID);
                        assertThat(history.getReason()).isEqualTo(REASON);
                    });
            softly.assertThat(logs).filteredOn(
                            log -> log.getEventType() == NotificationEventType.BOOKING_CANCELED)
                    .hasSize(4);
            softly.assertThat(logs).filteredOn(
                            log -> log.getEventType() == NotificationEventType.DEPOSIT_REFUNDED)
                    .hasSize(2);
        });
    }

    @DisplayName("관리자 활성 슬롯은 수업 일괄 취소를 거절하고 예약 상태를 유지한다")
    @Test
    void cancelSession_activeSlot_rejectedWithoutMutation() {
        Slot slot = slotStorePort.save(slot(bookingClass, FUTURE, FUTURE.plusHours(2)));
        Booking booking = saveGuestDepositBooking(slot, "03", false);
        occupy(slot, 1);

        assertThatThrownBy(() -> adminBookingCancelUseCase.cancelSession(
                new CancelSessionCommand(slot.getId(), ADMIN_ID, REASON)))
                .isInstanceOfSatisfying(HappyGalleryException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                    assertThat(error).hasMessageContaining("비활성 슬롯");
                });

        assertSoftly(softly -> {
            softly.assertThat(bookingStateProbe.getBooking(booking.getId()).getStatus())
                    .isEqualTo(BookingStatus.BOOKED);
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isEqualTo(1);
            softly.assertThat(bookingStateProbe.refundCount()).isZero();
            softly.assertThat(bookingHistoryRepository.count()).isZero();
        });
    }

    private Booking saveGuestDepositBooking(Slot slot, String suffix, boolean balancePaid) {
        Guest guest = guestStorePort.save(guest("예약자" + suffix, "010100000" + suffix));
        Booking booking = booking(
                guest,
                slot,
                5_000L,
                45_000L,
                DepositPaymentMethod.CARD,
                accessToken());
        booking.recordPaymentConfirmation("payment-key-" + suffix, LocalDateTime.now(clock));
        if (balancePaid) {
            booking.markBalancePaid(LocalDateTime.now(clock));
        }
        return bookingStorePort.save(booking);
    }

    private PassBooking savePassBooking(Slot slot, String suffix, String phone, LocalDateTime expiresAt) {
        User user = userStorePort.save(new User(
                suffix + "@example.com", "password-hash", "회원" + suffix, phone));
        PassPurchase pass = passPurchase(user.getId(), expiresAt, 320_000L);
        pass.useCredit(expiresAt.minusDays(1));
        pass = passPurchaseStorePort.save(pass);
        Booking booking = bookingStorePort.save(Booking.forMemberPass(user, slot, pass));
        return new PassBooking(booking, pass);
    }

    private void occupy(Slot slot, int count) {
        for (int i = 0; i < count; i++) {
            slot.incrementBookedCount();
        }
        slotStorePort.save(slot);
    }

    private List<Refund> awaitSucceededRefunds(int expectedCount) {
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(bookingStateProbe.refunds())
                        .hasSize(expectedCount)
                        .allSatisfy(refund -> assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED)));
        return bookingStateProbe.refunds();
    }

    private void cleanup() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearUsers();
    }

    private record PassBooking(Booking booking, PassPurchase pass) {}
}
