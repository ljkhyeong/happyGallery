package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.booking.BookingCancellationService;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase.PassRefundResult;
import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PassRefundTransactionService {

    private static final Logger log = LoggerFactory.getLogger(PassRefundTransactionService.class);

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final PassExpirationSupport expirationSupport;
    private final BookingCancellationService bookingCancellationService;
    private final RefundExecutionService refundExecutionService;

    PassRefundTransactionService(PassPurchaseReaderPort passPurchaseReader,
                                 PassPurchaseStorePort passPurchaseStore,
                                 PassLedgerStorePort passLedgerStore,
                                 PassExpirationSupport expirationSupport,
                                 BookingCancellationService bookingCancellationService,
                                 RefundExecutionService refundExecutionService) {
        this.passPurchaseReader = passPurchaseReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.expirationSupport = expirationSupport;
        this.bookingCancellationService = bookingCancellationService;
        this.refundExecutionService = refundExecutionService;
    }

    /** 만료 정규화가 필요한 경우 이를 커밋하고 빈 결과를 반환한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Optional<PassRefundResult> refundIfActive(Long passId) {
        PassPurchase pass = passPurchaseReader.findByIdForUpdate(passId)
                .orElseThrow(NotFoundException.supplier("8회권"));
        return refundLockedPass(pass);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Optional<PassRefundResult> refundOwnedIfActive(Long passId, Long userId) {
        PassPurchase pass = passPurchaseReader.findByIdForUpdate(passId)
                .filter(lockedPass -> Objects.equals(lockedPass.getUserId(), userId))
                .orElseThrow(NotFoundException.supplier("8회권"));
        return refundLockedPass(pass);
    }

    private Optional<PassRefundResult> refundLockedPass(PassPurchase pass) {
        if (expirationSupport.expireIfReached(pass).isPresent()) {
            return Optional.empty();
        }

        int cancelledCount = bookingCancellationService.cancelLinkedBookings(pass.getId());
        int refundCredits = Math.clamp(
                pass.getRemainingCredits() + cancelledCount, 0, pass.getTotalCredits());
        long refundAmount = pass.calculateRefundAmount(refundCredits);
        Refund refund = refundAmount > 0
                ? refundExecutionService.requestPassRefund(pass.getId(), refundAmount, pass.getPaymentKey())
                : null;

        if (refundCredits > 0) {
            passLedgerStore.save(new PassLedger(pass, PassLedgerType.REFUND, refundCredits));
        }
        pass.expire();
        passPurchaseStore.save(pass);

        log.info("Pass환불 처리 완료 [passId={}] 취소예약={}건, 환불크레딧={}, 환불금액={}, refundId={}, refundStatus={}",
                pass.getId(), cancelledCount, refundCredits, refundAmount,
                refund != null ? refund.getId() : null,
                refund != null ? refund.getStatus() : null);

        return Optional.of(new PassRefundResult(
                cancelledCount,
                refundCredits,
                refundAmount,
                refund != null ? refund.getId() : null,
                refund != null ? refund.getStatus() : null));
    }
}
