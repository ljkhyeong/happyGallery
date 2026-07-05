package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.RefundTransactionService.RefundCall;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 환불 요청 이력 저장 및 실행 예약 서비스.
 *
 * <p>환불 요청 레코드는 호출 유스케이스 트랜잭션에 참여하고, PG 환불 호출은 커밋 이후
 * 전용 executor에서 실행한다. 결과 업데이트는 짧은 REQUIRES_NEW 트랜잭션으로 저장한다.
 * 예약, 주문, 8회권 환불을 처리하는 공용 경계 서비스.
 */
@Service
public class RefundExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RefundExecutionService.class);
    private static final String MISSING_PAYMENT_KEY_REASON = "paymentKey가 없어 PG 환불을 실행할 수 없습니다.";

    private final RefundPort refundPort;
    private final PaymentPort paymentPort;
    private final RefundTransactionService refundTransactionService;
    private final Executor refundExecutor;

    public RefundExecutionService(RefundPort refundPort,
                                  PaymentPort paymentPort,
                                  RefundTransactionService refundTransactionService,
                                  @Qualifier("refundExecutor") Executor refundExecutor) {
        this.refundPort = refundPort;
        this.paymentPort = paymentPort;
        this.refundTransactionService = refundTransactionService;
        this.refundExecutor = refundExecutor;
    }

    /** 환불 요청 이력을 저장하고 커밋 이후 PG 환불 실행을 예약한다. 반환값은 PG 결과 반영 전 요청 이력이다. */
    public Refund requestOrderRefund(Long orderId, long amount, String paymentKey) {
        Refund refund = refundPort.save(Refund.forOrder(orderId, amount, paymentKey));
        scheduleAfterCommit(refund.getId(), "orderId=" + orderId);
        return refund;
    }

    /** 환불 요청 이력을 저장하고 커밋 이후 PG 환불 실행을 예약한다. 반환값은 PG 결과 반영 전 요청 이력이다. */
    public Refund requestBookingRefund(Booking booking, long amount) {
        Refund refund = refundPort.save(Refund.forBooking(booking, amount));
        scheduleAfterCommit(refund.getId(), "bookingId=" + booking.getId());
        return refund;
    }

    /** 환불 요청 이력을 저장하고 커밋 이후 PG 환불 실행을 예약한다. 반환값은 PG 결과 반영 전 요청 이력이다. */
    public Refund requestPassRefund(Long passPurchaseId, long amount, String paymentKey) {
        Refund refund = refundPort.save(Refund.forPass(passPurchaseId, amount, paymentKey));
        scheduleAfterCommit(refund.getId(), "passPurchaseId=" + passPurchaseId);
        return refund;
    }

    public Refund retryRefund(Long refundId) {
        refundTransactionService.validateRetryable(refundId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            scheduleAfterCommit(refundId, "retry refundId=" + refundId);
            return refundTransactionService.find(refundId);
        }
        return executeRefund(refundId, "retry refundId=" + refundId);
    }

    private void scheduleAfterCommit(Long refundId, String target) {
        Runnable refundTask = () -> executeRefund(refundId, target);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refundExecutor.execute(refundTask);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refundExecutor.execute(refundTask);
            }
        });
    }

    private Refund executeRefund(Long refundId, String target) {
        RefundCall refundCall = refundTransactionService.prepareRefundCall(refundId, MISSING_PAYMENT_KEY_REASON);
        if (refundCall.failedBeforePgCall()) {
            log.warn("환불 실패 [{} refundId={}] reason=paymentKey 없음", target, refundId);
            return refundCall.failedRefund();
        }

        RefundResult result = callPayment(refundCall, target);
        if (result.success()) {
            return refundTransactionService.markSucceeded(refundId, result.refundTransactionKey());
        }
        log.warn("환불 실패 [{} refundId={}] reason={}", target, refundId, result.failReason());
        return refundTransactionService.markFailed(refundId, result.failReason());
    }

    private RefundResult callPayment(RefundCall refundCall, String target) {
        try {
            RefundResult result = paymentPort.refund(refundCall.paymentKey(), refundCall.amount());
            return result != null ? result : RefundResult.failure("PG 응답이 비어 있습니다.");
        } catch (Exception e) {
            log.error("환불 호출 예외 [{} refundId={}]", target, refundCall.refundId(), e);
            return RefundResult.failure(e.getMessage());
        }
    }
}
