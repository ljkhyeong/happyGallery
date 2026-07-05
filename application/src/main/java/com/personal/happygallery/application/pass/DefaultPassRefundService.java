package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.booking.BookingCancellationService;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.booking.Refund;
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
    private final BookingCancellationService bookingCancellationService;
    private final RefundExecutionService refundExecutionService;

    public DefaultPassRefundService(PassPurchaseReaderPort passPurchaseReader,
                                    PassPurchaseStorePort passPurchaseStore,
                                    PassLedgerStorePort passLedgerStore,
                                    BookingCancellationService bookingCancellationService,
                                    RefundExecutionService refundExecutionService) {
        this.passPurchaseReader = passPurchaseReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.bookingCancellationService = bookingCancellationService;
        this.refundExecutionService = refundExecutionService;
    }

    /**
     * 8회권 전체 환불. 관리자 호출.
     *
     * <ol>
     *   <li>미래 BOOKED 예약 자동 취소 (슬롯 booked_count--, 이력 기록)</li>
     *   <li>PG 환불 요청 이력 기록 및 커밋 이후 환불 실행 예약</li>
     *   <li>REFUND ledger 기록 (amount = remaining_credits)</li>
     *   <li>remaining_credits = 0 (expire() 재활용)</li>
     * </ol>
     *
     * <p>PG 환불 실패 시 환불 이력은 비동기로 FAILED가 되어 운영자 재시도 대상이 된다.
     *
     * @return 처리 결과 (취소된 예약 수, 환불 크레딧, 환불 금액, 환불 이력)
     */
    public PassRefundResult refundPass(Long passId) {
        PassPurchase pass = passPurchaseReader.findById(passId)
                .orElseThrow(NotFoundException.supplier("8회권"));

        // 1. 미래 BOOKED 예약 자동 취소
        int cancelledCount = bookingCancellationService.cancelLinkedBookings(passId);

        // 2. PG 환불 요청
        int refundCredits = pass.getRemainingCredits();
        long refundAmount = pass.calculateRefundAmount();
        Refund refund = null;

        if (refundAmount > 0) {
            refund = refundExecutionService.requestPassRefund(pass.getId(), refundAmount, pass.getPaymentKey());
        }

        // 3. REFUND ledger 기록 (잔여 크레딧 전체)
        if (refundCredits > 0) {
            passLedgerStore.save(new PassLedger(pass, PassLedgerType.REFUND, refundCredits));
        }

        // 4. 잔여 크레딧 0으로 소멸
        pass.expire();
        passPurchaseStore.save(pass);

        log.info("Pass환불 처리 완료 [passId={}] 취소예약={}건, 환불크레딧={}, 환불금액={}, refundId={}, refundStatus={}",
                passId, cancelledCount, refundCredits, refundAmount,
                refund != null ? refund.getId() : null,
                refund != null ? refund.getStatus() : null);

        return new PassRefundResult(
                cancelledCount,
                refundCredits,
                refundAmount,
                refund != null ? refund.getId() : null,
                refund != null ? refund.getStatus() : null);
    }
}
