package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.RefundTransactionService.RefundCall;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class RefundDispatcher {

    private static final Logger log = LoggerFactory.getLogger(RefundDispatcher.class);

    private final PaymentPort paymentPort;
    private final RefundTransactionService transactionService;

    RefundDispatcher(PaymentPort paymentPort, RefundTransactionService transactionService) {
        this.paymentPort = paymentPort;
        this.transactionService = transactionService;
    }

    @Transactional(propagation = Propagation.NEVER)
    public Refund dispatch(Long refundId, String target) {
        RefundCall refundCall = transactionService.claimRefundCall(refundId);
        if (!refundCall.readyForPgCall()) {
            return refundCall.completedRefund();
        }

        RefundResult result = callPayment(refundCall, target);
        return switch (result.outcome()) {
            case SUCCESS -> transactionService.markSucceeded(
                    refundId, refundCall.processingToken(), result.refundTransactionKey());
            case FINAL_FAILURE -> {
                log.warn("환불 최종 실패 [{} refundId={}] reason={}", target, refundId, result.failReason());
                yield transactionService.markFailed(refundId, refundCall.processingToken(), result.failReason());
            }
            case RETRYABLE_FAILURE -> {
                log.warn("환불 재시도 예약 [{} refundId={}] reason={}", target, refundId, result.failReason());
                yield transactionService.markRetryable(refundId, refundCall.processingToken(), result.failReason());
            }
            case RECONCILIATION_REQUIRED -> {
                log.warn("환불 상태 확인 필요 [{} refundId={}] reason={}", target, refundId, result.failReason());
                yield transactionService.markReconciliationRequired(
                        refundId, refundCall.processingToken(), result.failReason());
            }
        };
    }

    private RefundResult callPayment(RefundCall refundCall, String target) {
        try {
            RefundResult result = paymentPort.refund(
                    refundCall.paymentKey(), refundCall.amount(), refundCall.idempotencyKey());
            return result != null
                    ? result
                    : RefundResult.reconciliationRequired("PG 응답이 비어 있어 환불 상태 확인이 필요합니다.");
        } catch (Exception e) {
            log.error("환불 호출 예외 [{} refundId={} type={}]",
                    target, refundCall.refundId(), e.getClass().getSimpleName());
            return RefundResult.reconciliationRequired("PG 호출 결과를 확인할 수 없습니다.");
        }
    }
}
