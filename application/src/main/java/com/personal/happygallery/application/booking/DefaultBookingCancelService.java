package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.pass.PassCreditService;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.domain.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBookingCancelService implements BookingCancelUseCase {

    private final BookingStorePort bookingStorePort;
    private final RefundExecutionService refundExecutionService;
    private final PassCreditService passCreditService;
    private final SlotCapacitySupport slotCapacitySupport;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public DefaultBookingCancelService(BookingStorePort bookingStorePort,
                                RefundExecutionService refundExecutionService,
                                PassCreditService passCreditService,
                                SlotCapacitySupport slotCapacitySupport,
                                BookingSupport bookingSupport,
                                Clock clock) {
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

    private CancelResult cancelInternal(Booking booking) {
        booking.cancel();

        // 1. 슬롯 반납 — 비관적 락 + booked_count--
        Slot slot = slotCapacitySupport.releaseCapacity(booking.getSlot().getId());

        // 2. CANCELED 이력 저장 (append-only)
        bookingSupport.recordHistory(booking, BookingHistoryAction.CANCELED, slot, null, "CUSTOMER", null);

        // 3. 환불/크레딧 복구 등 취소 보상 처리
        CancellationCompensation compensation = applyCancellationCompensation(booking, slot);

        // 4. 예약 취소 저장
        bookingStorePort.save(booking);

        // 5. 취소 알림. 실제 예약금 환불 성공 알림은 커밋 이후 RefundExecutionService가 발행한다.
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_CANCELED);

        return new CancelResult(booking, compensation.refundable(), compensation.refund());
    }

    private CancellationCompensation applyCancellationCompensation(Booking booking, Slot slot) {
        boolean refundable = TimeBoundary.isRefundable(slot.getStartAt(), clock);
        if (!refundable) {
            return new CancellationCompensation(false, null);
        }

        if (booking.isPassBooking()) {
            restorePassCredit(booking);
            return new CancellationCompensation(true, null);
        }

        Refund refund = refundExecutionService.requestBookingRefund(booking, booking.getDepositAmount());
        return new CancellationCompensation(true, refund);
    }

    private void restorePassCredit(Booking booking) {
        passCreditService.restoreCredit(booking.getPassPurchase().getId(), booking.getId());
    }

    private record CancellationCompensation(boolean refundable, Refund refund) {}
}
