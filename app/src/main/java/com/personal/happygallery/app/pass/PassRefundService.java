package com.personal.happygallery.app.pass;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PassRefundService {

    private static final Logger log = LoggerFactory.getLogger(PassRefundService.class);

    private final PassPurchaseRepository passPurchaseRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SlotRepository slotRepository;
    private final Clock clock;

    public PassRefundService(PassPurchaseRepository passPurchaseRepository,
                             PassLedgerRepository passLedgerRepository,
                             BookingRepository bookingRepository,
                             BookingHistoryRepository bookingHistoryRepository,
                             SlotRepository slotRepository,
                             Clock clock) {
        this.passPurchaseRepository = passPurchaseRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.slotRepository = slotRepository;
        this.clock = clock;
    }

    /**
     * 8회권 전체 환불. 관리자 수동 호출.
     *
     * <ol>
     *   <li>미래 BOOKED 예약 자동 취소 (슬롯 booked_count--, 이력 기록)</li>
     *   <li>REFUND ledger 기록 (amount = remaining_credits)</li>
     *   <li>remaining_credits = 0 (expire() 재활용)</li>
     * </ol>
     *
     * <p>실제 PG 환불은 관리자가 {@code refundAmount}를 참고해 수동 처리한다.
     *
     * @return 처리 결과 (취소된 예약 수, 환불 크레딧, 환불 금액 계산값)
     */
    public PassRefundResult refundPass(Long passId) {
        PassPurchase pass = passPurchaseRepository.findById(passId)
                .orElseThrow(() -> new NotFoundException("8회권"));

        // 1. 미래 BOOKED 예약 자동 취소
        List<Booking> futureBookings = bookingRepository.findFuturePassBookings(
                passId, BookingStatus.BOOKED, LocalDateTime.now(clock));

        for (Booking booking : futureBookings) {
            var slot = slotRepository.findByIdWithLock(booking.getSlot().getId())
                    .orElseThrow(() -> new NotFoundException("슬롯"));
            slot.decrementBookedCount();
            slotRepository.save(slot);

            bookingHistoryRepository.save(
                    new BookingHistory(booking, BookingHistoryAction.CANCELED,
                            slot, null, "ADMIN", null));

            booking.cancel();
            bookingRepository.save(booking);
            log.info("Pass환불 연동 취소 [passId={}, bookingId={}]", passId, booking.getId());
        }

        // 2. REFUND ledger 기록 (잔여 크레딧 전체)
        int refundCredits = pass.getRemainingCredits();
        long refundAmount = refundCredits * pass.unitPrice();

        if (refundCredits > 0) {
            passLedgerRepository.save(new PassLedger(pass, PassLedgerType.REFUND, refundCredits));
        }

        // 3. 잔여 크레딧 0으로 소멸
        pass.expire();
        passPurchaseRepository.save(pass);

        log.info("Pass환불 완료 [passId={}] 취소예약={}건, 환불크레딧={}, 환불금액={}",
                passId, futureBookings.size(), refundCredits, refundAmount);

        return new PassRefundResult(futureBookings.size(), refundCredits, refundAmount);
    }

    public record PassRefundResult(int canceledBookings, int refundCredits, long refundAmount) {}
}
