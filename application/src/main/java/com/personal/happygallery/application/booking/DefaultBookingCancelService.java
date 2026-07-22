package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelCommand;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelResult;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionCommand;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionResult;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.pass.PassCreditService;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.BookingConflictException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBookingCancelService implements BookingCancelUseCase, AdminBookingCancelUseCase {

    private final BookingReaderPort bookingReaderPort;
    private final BookingStorePort bookingStorePort;
    private final RefundExecutionService refundExecutionService;
    private final PassCreditService passCreditService;
    private final SlotCapacitySupport slotCapacitySupport;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public DefaultBookingCancelService(BookingReaderPort bookingReaderPort,
                                       BookingStorePort bookingStorePort,
                                       RefundExecutionService refundExecutionService,
                                       PassCreditService passCreditService,
                                       SlotCapacitySupport slotCapacitySupport,
                                       BookingSupport bookingSupport,
                                       Clock clock) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingStorePort = bookingStorePort;
        this.refundExecutionService = refundExecutionService;
        this.passCreditService = passCreditService;
        this.slotCapacitySupport = slotCapacitySupport;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    /**
     * 비회원 예약을 취소한다. 8회권/예약금 결제 경로를 분기 처리한다.
     *
     * <ol>
     *   <li>access_token으로 예약 조회 및 검증</li>
     *   <li>상태 확인 → 슬롯 반납 → 이력 → 환불 → 취소 → 알림</li>
     * </ol>
     *
     * @return 취소된 예약, 보상 가능 여부, 생성된 예약금 환불 요청
     */
    @Override
    public CancelResult cancelBooking(Long bookingId, String accessToken) {
        Booking booking = bookingSupport.findByToken(bookingId, accessToken);
        return cancelInternal(booking);
    }

    /**
     * 회원 예약을 취소한다. accessToken 대신 userId 소유권으로 검증한다.
     */
    @Override
    public CancelResult cancelMemberBooking(Long bookingId, Long userId) {
        Booking booking = bookingSupport.findByIdAndUserId(bookingId, userId);
        return cancelInternal(booking);
    }

    @Override
    public AdminCancelResult cancel(AdminCancelCommand command) {
        Booking booking = bookingSupport.findById(command.bookingId());
        PassPurchase lockedPass = lockPass(booking);
        SlotCapacitySupport.LockedSlotScope lockedSlot =
                slotCapacitySupport.lockCapacityScope(booking.getSlot().getId());
        booking.cancelByAdmin();

        Slot slot = slotCapacitySupport.releaseCapacity(lockedSlot);
        return completeAdminCancellation(
                booking, lockedPass, slot, command.adminId(), command.reason());
    }

    @Override
    public CancelSessionResult cancelSession(CancelSessionCommand command) {
        Map<Long, PassPurchase> lockedPasses = lockPasses(
                bookingReaderPort.findBookedPassIdsBySlotId(command.slotId()));
        SlotCapacitySupport.LockedSlotScope lockedSlot =
                slotCapacitySupport.lockInactiveSessionSlot(command.slotId());
        List<Booking> bookings = bookingReaderPort.findBookedBySlotIdForUpdate(command.slotId());
        if (bookings.stream()
                .filter(Booking::isPassBooking)
                .map(booking -> booking.getPassPurchase().getId())
                .anyMatch(passId -> !lockedPasses.containsKey(passId))) {
            throw new BookingConflictException();
        }

        int passCreditsRestored = 0;
        int depositRefundsRequested = 0;
        int balanceSettlementsRequired = 0;
        int manualCompensationsRequired = 0;

        for (Booking booking : bookings) {
            PassPurchase lockedPass = booking.isPassBooking()
                    ? lockedPasses.get(booking.getPassPurchase().getId())
                    : null;
            booking.cancelByAdmin();
            Slot slot = slotCapacitySupport.releaseCapacity(lockedSlot);
            AdminCancelResult result = completeAdminCancellation(
                    booking, lockedPass, slot, command.adminId(), command.reason());

            if (booking.isPassBooking()) {
                if (result.passCreditRestored()) {
                    passCreditsRestored++;
                } else {
                    manualCompensationsRequired++;
                }
            } else if (result.refund() != null) {
                depositRefundsRequested++;
            }
            if (result.balanceSettlementRequired()) {
                balanceSettlementsRequired++;
            }
        }

        return new CancelSessionResult(
                bookings.size(),
                passCreditsRestored,
                depositRefundsRequested,
                balanceSettlementsRequired,
                manualCompensationsRequired);
    }

    private AdminCancelResult completeAdminCancellation(
            Booking booking,
            PassPurchase lockedPass,
            Slot slot,
            Long adminId,
            String reason
    ) {
        bookingSupport.recordHistory(
                booking,
                BookingHistoryAction.CANCELED,
                slot,
                null,
                "ADMIN",
                adminId,
                reason.trim());

        CancellationCompensation compensation = applyAdminCancellationCompensation(booking, lockedPass);
        bookingStorePort.save(booking);
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_CANCELED);

        boolean balanceSettlementRequired = booking.getBalanceAmount() > 0
                && booking.getBalanceStatus() == BalanceStatus.PAID;
        return new AdminCancelResult(
                booking,
                compensation.refundable() && lockedPass != null,
                compensation.refund(),
                balanceSettlementRequired);
    }

    private CancelResult cancelInternal(Booking booking) {
        PassPurchase lockedPass = lockPass(booking);
        SlotCapacitySupport.LockedSlotScope lockedSlot =
                slotCapacitySupport.lockCapacityScope(booking.getSlot().getId());
        booking.cancel();

        // 1. 슬롯 반납 — 비관적 락 + booked_count--
        Slot slot = slotCapacitySupport.releaseCapacity(lockedSlot);

        // 2. CANCELED 이력 저장 (append-only)
        bookingSupport.recordHistory(
                booking, BookingHistoryAction.CANCELED, slot, null, "CUSTOMER", null, null);

        // 3. 환불/크레딧 복구 등 취소 보상 처리
        CancellationCompensation compensation = applyCancellationCompensation(booking, slot, lockedPass);

        // 4. 예약 취소 저장
        bookingStorePort.save(booking);

        // 5. 취소 알림. 실제 예약금 환불 성공 알림은 커밋 이후 RefundExecutionService가 발행한다.
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_CANCELED);

        return new CancelResult(booking, compensation.refundable(), compensation.refund());
    }

    private PassPurchase lockPass(Booking booking) {
        return booking.isPassBooking()
                ? passCreditService.requireForUpdate(booking.getPassPurchase().getId())
                : null;
    }

    private Map<Long, PassPurchase> lockPasses(List<Long> candidatePassIds) {
        List<Long> passIds = candidatePassIds.stream()
                .distinct()
                .sorted()
                .toList();
        Map<Long, PassPurchase> lockedPasses = new HashMap<>();
        for (Long passId : passIds) {
            lockedPasses.put(passId, passCreditService.requireForUpdate(passId));
        }
        return lockedPasses;
    }

    private CancellationCompensation applyCancellationCompensation(Booking booking,
                                                                    Slot slot,
                                                                    PassPurchase lockedPass) {
        boolean refundable = TimeBoundary.isRefundable(slot.getStartAt(), clock);
        if (!refundable) {
            return new CancellationCompensation(false, null);
        }

        if (lockedPass != null) {
            boolean restored = passCreditService.restoreCredit(lockedPass, booking.getId());
            return new CancellationCompensation(restored, null);
        }

        Refund refund = refundExecutionService.requestBookingRefund(booking, booking.getDepositAmount());
        return new CancellationCompensation(true, refund);
    }

    private CancellationCompensation applyAdminCancellationCompensation(
            Booking booking,
            PassPurchase lockedPass
    ) {
        if (lockedPass != null) {
            boolean restored = passCreditService.restoreCredit(lockedPass, booking.getId());
            return new CancellationCompensation(restored, null);
        }
        Refund refund = refundExecutionService.requestBookingRefund(booking, booking.getDepositAmount());
        return new CancellationCompensation(true, refund);
    }

    private record CancellationCompensation(boolean refundable, Refund refund) {}
}
