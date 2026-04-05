package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.booking.port.in.BookingCancellationUseCase;
import com.personal.happygallery.app.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.app.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPassRefundService implements PassRefundUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPassRefundService.class);

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final BookingCancellationUseCase bookingCancellationPort;

    public DefaultPassRefundService(PassPurchaseReaderPort passPurchaseReader,
                             PassPurchaseStorePort passPurchaseStore,
                             PassLedgerStorePort passLedgerStore,
                             BookingCancellationUseCase bookingCancellationPort) {
        this.passPurchaseReader = passPurchaseReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.bookingCancellationPort = bookingCancellationPort;
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
                .orElseThrow(NotFoundException.supplier("8회권"));

        // 1. 미래 BOOKED 예약 자동 취소
        int cancelledCount = bookingCancellationPort.cancelLinkedBookings(passId);

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
                passId, cancelledCount, refundCredits, refundAmount);

        return new PassRefundResult(cancelledCount, refundCredits, refundAmount);
    }
}
