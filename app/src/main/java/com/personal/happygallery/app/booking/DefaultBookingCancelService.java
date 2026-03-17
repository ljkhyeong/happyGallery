package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.app.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingCancelService {

    private final SlotReaderPort slotReaderPort;
    private final SlotStorePort slotStorePort;
    private final BookingStorePort bookingStorePort;
    private final RefundExecutionService refundExecutionService;
    private final PassPurchaseStorePort passPurchaseStorePort;
    private final PassLedgerStorePort passLedgerStorePort;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public BookingCancelService(SlotReaderPort slotReaderPort,
                                SlotStorePort slotStorePort,
                                BookingStorePort bookingStorePort,
                                RefundExecutionService refundExecutionService,
                                PassPurchaseStorePort passPurchaseStorePort,
                                PassLedgerStorePort passLedgerStorePort,
                                BookingSupport bookingSupport,
                                Clock clock) {
        this.slotReaderPort = slotReaderPort;
        this.slotStorePort = slotStorePort;
        this.bookingStorePort = bookingStorePort;
        this.refundExecutionService = refundExecutionService;
        this.passPurchaseStorePort = passPurchaseStorePort;
        this.passLedgerStorePort = passLedgerStorePort;
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
     * @return (취소된 booking, 환불 가능 여부)
     */
    public CancelResult cancelBooking(Long bookingId, String accessToken) {
        Booking booking = bookingSupport.findByToken(bookingId, accessToken);
        return cancelInternal(booking, bookingSupport::notifyBookingGuest);
    }

    /**
     * 회원 예약을 취소한다. accessToken 대신 userId 소유권으로 검증한다.
     */
    public CancelResult cancelMemberBooking(Long bookingId, Long userId) {
        Booking booking = bookingSupport.findByIdAndUserId(bookingId, userId);
        return cancelInternal(booking, bookingSupport::notifyBookingUser);
    }

    private CancelResult cancelInternal(Booking booking,
                                         BiConsumer<Booking, NotificationEventType> notify) {
        // 1. 상태 체크 — BOOKED 상태만 취소 가능
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "취소할 수 없는 예약 상태입니다.");
        }

        // 2. 슬롯 반납 — 비관적 락 + booked_count--
        Slot slot = slotReaderPort.findByIdWithLock(booking.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("슬롯"));
        slot.decrementBookedCount();
        slotStorePort.save(slot);

        // 3. CANCELED 이력 저장 (append-only)
        bookingSupport.recordHistory(booking, BookingHistoryAction.CANCELED, slot, null, "CUSTOMER", null);

        // 4. 환불 가능 여부 판단 (D-1 00:00 Asia/Seoul 기준)
        boolean refundable = TimeBoundary.isRefundable(slot.getStartAt(), clock);

        if (booking.isPassBooking()) {
            if (refundable) {
                PassPurchase pass = booking.getPassPurchase();
                passLedgerStorePort.save(
                        new PassLedger(pass, PassLedgerType.REFUND, 1, booking.getId()));
                pass.refundCredit();
                passPurchaseStorePort.save(pass);
            }
        } else {
            if (refundable) {
                refundExecutionService.processBookingRefund(booking.getId(), booking.getDepositAmount());
            }
        }

        // 5. 예약 취소 처리
        booking.cancel();
        bookingStorePort.save(booking);

        // 6. 취소 알림 (+ 예약금 환불 시 환불 알림)
        notify.accept(booking, NotificationEventType.BOOKING_CANCELED);
        if (refundable && !booking.isPassBooking()) {
            notify.accept(booking, NotificationEventType.DEPOSIT_REFUNDED);
        }

        return new CancelResult(booking, refundable);
    }

    public record CancelResult(Booking booking, boolean refundable) {}
}
