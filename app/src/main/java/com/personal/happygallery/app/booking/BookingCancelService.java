package com.personal.happygallery.app.booking;

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
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingCancelService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final RefundExecutionService refundExecutionService;
    private final PassPurchaseRepository passPurchaseRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public BookingCancelService(BookingRepository bookingRepository,
                                SlotRepository slotRepository,
                                RefundExecutionService refundExecutionService,
                                PassPurchaseRepository passPurchaseRepository,
                                PassLedgerRepository passLedgerRepository,
                                BookingSupport bookingSupport,
                                Clock clock) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.refundExecutionService = refundExecutionService;
        this.passPurchaseRepository = passPurchaseRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    /**
     * 비회원 예약을 취소한다. 8회권/예약금 결제 경로를 분기 처리한다.
     *
     * <ol>
     *   <li>access_token으로 예약 조회 및 검증</li>
     *   <li>BOOKED 상태 확인</li>
     *   <li>슬롯 반납: 비관적 락 + booked_count--</li>
     *   <li>CANCELED 이력 저장</li>
     *   <li>8회권: D-1 이전이면 REFUND ledger(+1) + refundCredit(). 이후면 크레딧 소멸.</li>
     *   <li>예약금: D-1 이전이면 PG 환불 요청(Refund REQUESTED).</li>
     *   <li>booking.cancel() + save</li>
     * </ol>
     *
     * @return (취소된 booking, 환불 가능 여부)
     */
    public CancelResult cancelBooking(Long bookingId, String accessToken) {

        // 1. 예약 로드 (accessToken 검증)
        Booking booking = bookingSupport.findByToken(bookingId, accessToken);

        // 2. 상태 체크 — BOOKED 상태만 취소 가능
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "취소할 수 없는 예약 상태입니다.");
        }

        // 3. 슬롯 반납 — 비관적 락 + booked_count--
        Slot slot = slotRepository.findByIdWithLock(booking.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("슬롯"));
        slot.decrementBookedCount();
        slotRepository.save(slot);

        // 4. CANCELED 이력 저장 (append-only)
        bookingSupport.recordHistory(booking, BookingHistoryAction.CANCELED, slot, null, "CUSTOMER", null);

        // 5. 환불 가능 여부 판단 (D-1 00:00 Asia/Seoul 기준)
        boolean refundable = TimeBoundary.isRefundable(slot.getStartAt(), clock);

        if (booking.isPassBooking()) {
            // 5a. 8회권 결제 취소 — D-1 이전이면 크레딧 복구, 이후면 소멸 유지
            if (refundable) {
                PassPurchase pass = booking.getPassPurchase();
                passLedgerRepository.save(
                        new PassLedger(pass, PassLedgerType.REFUND, 1, booking.getId()));
                pass.refundCredit();
                passPurchaseRepository.save(pass);
            }
        } else {
            // 5b. 예약금 결제 취소 — D-1 이전이면 PG 환불 요청
            if (refundable) {
                refundExecutionService.processBookingRefund(booking.getId(), booking.getDepositAmount());
            }
        }

        // 6. 예약 취소 처리
        booking.cancel();
        bookingRepository.save(booking);

        // 7. 취소 알림 (+ 예약금 환불 시 환불 알림)
        bookingSupport.notifyBookingGuest(booking, NotificationEventType.BOOKING_CANCELED);
        if (refundable && !booking.isPassBooking()) {
            bookingSupport.notifyBookingGuest(booking, NotificationEventType.DEPOSIT_REFUNDED);
        }

        return new CancelResult(booking, refundable);
    }

    public record CancelResult(Booking booking, boolean refundable) {}
}
