package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.app.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.personal.happygallery.app.pass.port.in.PassRefundUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPassRefundService implements PassRefundUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPassRefundService.class);

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final BookingReaderPort bookingReader;
    private final BookingStorePort bookingStore;
    private final BookingHistoryPort bookingHistoryPort;
    private final SlotReaderPort slotReader;
    private final SlotStorePort slotStore;
    private final Clock clock;

    public DefaultPassRefundService(PassPurchaseReaderPort passPurchaseReader,
                             PassPurchaseStorePort passPurchaseStore,
                             PassLedgerStorePort passLedgerStore,
                             BookingReaderPort bookingReader,
                             BookingStorePort bookingStore,
                             BookingHistoryPort bookingHistoryPort,
                             SlotReaderPort slotReader,
                             SlotStorePort slotStore,
                             Clock clock) {
        this.passPurchaseReader = passPurchaseReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.bookingReader = bookingReader;
        this.bookingStore = bookingStore;
        this.bookingHistoryPort = bookingHistoryPort;
        this.slotReader = slotReader;
        this.slotStore = slotStore;
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
        PassPurchase pass = passPurchaseReader.findById(passId)
                .orElseThrow(() -> new NotFoundException("8회권"));

        // 1. 미래 BOOKED 예약 자동 취소
        List<Booking> futureBookings = bookingReader.findFuturePassBookings(
                passId, BookingStatus.BOOKED, LocalDateTime.now(clock));

        for (Booking booking : futureBookings) {
            var slot = slotReader.findByIdWithLock(booking.getSlot().getId())
                    .orElseThrow(() -> new NotFoundException("슬롯"));
            slot.decrementBookedCount();
            slotStore.save(slot);

            bookingHistoryPort.save(
                    new BookingHistory(booking, BookingHistoryAction.CANCELED,
                            slot, null, "ADMIN", null));

            booking.cancel();
            bookingStore.save(booking);
            log.info("Pass환불 연동 취소 [passId={}, bookingId={}]", passId, booking.getId());
        }

        // 2. REFUND ledger 기록 (잔여 크레딧 전체)
        int refundCredits = pass.getRemainingCredits();
        long refundAmount = pass.calculateRefundAmount();

        if (refundCredits > 0) {
            passLedgerStore.save(new PassLedger(pass, PassLedgerType.REFUND, refundCredits));
        }

        // 3. 잔여 크레딧 0으로 소멸
        pass.expire();
        passPurchaseStore.save(pass);

        log.info("Pass환불 완료 [passId={}] 취소예약={}건, 환불크레딧={}, 환불금액={}",
                passId, futureBookings.size(), refundCredits, refundAmount);

        return new PassRefundResult(futureBookings.size(), refundCredits, refundAmount);
    }

}
