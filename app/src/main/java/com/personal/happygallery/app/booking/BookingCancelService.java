package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.time.Clocks;
import com.personal.happygallery.common.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.payment.PaymentProvider;
import com.personal.happygallery.infra.payment.RefundResult;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingCancelService {

    private static final Logger log = LoggerFactory.getLogger(BookingCancelService.class);

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SlotRepository slotRepository;
    private final RefundRepository refundRepository;
    private final PaymentProvider paymentProvider;
    private final Clock clock;

    public BookingCancelService(BookingRepository bookingRepository,
                                BookingHistoryRepository bookingHistoryRepository,
                                SlotRepository slotRepository,
                                RefundRepository refundRepository,
                                PaymentProvider paymentProvider,
                                Clock clock) {
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.slotRepository = slotRepository;
        this.refundRepository = refundRepository;
        this.paymentProvider = paymentProvider;
        this.clock = clock;
    }

    /**
     * 비회원 예약을 취소한다.
     *
     * <ol>
     *   <li>access_token으로 예약 조회 및 검증</li>
     *   <li>BOOKED 상태 확인</li>
     *   <li>슬롯 반납: 비관적 락 + booked_count--</li>
     *   <li>CANCELED 이력 저장</li>
     *   <li>D-1 00:00 이전이면 환불 요청(Refund REQUESTED) 기록</li>
     *   <li>booking.cancel() + save</li>
     * </ol>
     *
     * @return (취소된 booking, 환불 가능 여부)
     */
    public CancelResult cancelBooking(Long bookingId, String accessToken) {

        // 1. 예약 로드 (accessToken 검증)
        Booking booking = bookingRepository.findByIdAndAccessToken(bookingId, accessToken)
                .orElseThrow(() -> new NotFoundException("예약"));

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
        bookingHistoryRepository.save(
                new BookingHistory(booking, BookingHistoryAction.CANCELED,
                        slot, null, "CUSTOMER", null));

        // 5. 환불 가능 여부 판단 (D-1 00:00 Asia/Seoul 기준)
        boolean refundable = TimeBoundary.isRefundable(
                slot.getStartAt().toLocalDate(), clock);

        if (refundable) {
            Refund refund = refundRepository.save(new Refund(booking, booking.getDepositAmount()));
            try {
                RefundResult result = paymentProvider.refund(refund.getPgRef(), refund.getAmount());
                if (result.success()) {
                    refund.markSucceeded(result.pgRef());
                } else {
                    log.warn("환불 실패 [refundId={}] reason={}", refund.getId(), result.failReason());
                    refund.markFailed(result.failReason());
                }
            } catch (Exception e) {
                log.error("환불 호출 예외 [refundId={}]", refund.getId(), e);
                refund.markFailed(e.getMessage());
            }
            refundRepository.save(refund);
        }

        // 6. 예약 취소 처리
        booking.cancel();
        bookingRepository.save(booking);

        return new CancelResult(booking, refundable);
    }

    public record CancelResult(Booking booking, boolean refundable) {}
}
