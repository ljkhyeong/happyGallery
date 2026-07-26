package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.RefundTransactionService.RefundCall;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.Refund;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        return dispatch(transactionService.claimRefundCall(refundId), target);
    }

    @Transactional(propagation = Propagation.NEVER)
    public Refund dispatchRecovery(Long refundId, String target) {
        return dispatch(transactionService.claimRefundCallForRecovery(refundId), target);
    }

    private Refund dispatch(RefundCall refundCall, String target) {
        return switch (refundCall) {
            case RefundCall.CancelRequired required -> dispatchCancel(required, target);
            case RefundCall.LookupRequired required -> dispatchLookup(required, target);
            case RefundCall.Skipped skipped -> skipped.refund();
        };
    }

    private Refund dispatchCancel(RefundCall.CancelRequired refundCall, String target) {
        RefundResult result = callRefund(refundCall, target);
        return switch (result.outcome()) {
            case SUCCESS -> transactionService.markSucceeded(
                    refundCall.refundId(), refundCall.processingToken(), result.refundTransactionKey());
            case FINAL_FAILURE -> {
                log.warn("환불 최종 실패 [{} refundId={}] reason={}",
                        target, refundCall.refundId(), result.failReason());
                yield transactionService.markFailed(
                        refundCall.refundId(), refundCall.processingToken(), result.failReason());
            }
            case RETRYABLE_FAILURE -> {
                log.warn("환불 재시도 예약 [{} refundId={}] reason={}",
                        target, refundCall.refundId(), result.failReason());
                yield transactionService.markRetryable(
                        refundCall.refundId(), refundCall.processingToken(), result.failReason());
            }
            case RECONCILIATION_REQUIRED -> {
                log.warn("환불 상태 확인 필요 [{} refundId={}] reason={}",
                        target, refundCall.refundId(), result.failReason());
                yield transactionService.markReconciliationRequired(
                        refundCall.refundId(), refundCall.processingToken(), result.failReason());
            }
        };
    }

    private Refund dispatchLookup(RefundCall.LookupRequired refundCall, String target) {
        RefundLookupResult result = lookupRefund(refundCall, target);
        return switch (result.status()) {
            case REFUNDED -> completeReconciledRefund(refundCall, result);
            case NOT_REFUNDED -> transactionService.markRetryable(
                    refundCall.refundId(), refundCall.processingToken(), result.reason());
            case REVIEW_REQUIRED, UNAVAILABLE -> transactionService.markReconciliationRequired(
                    refundCall.refundId(), refundCall.processingToken(), result.reason());
        };
    }

    private Refund completeReconciledRefund(RefundCall.LookupRequired refundCall, RefundLookupResult result) {
        if (!Objects.equals(refundCall.paymentKey(), result.paymentKey())
                || refundCall.amount() != result.cancelAmount()
                || !StringUtils.hasText(result.refundTransactionKey())) {
            return transactionService.markReconciliationRequired(
                    refundCall.refundId(),
                    refundCall.processingToken(),
                    "PG 환불 조회 결과가 저장된 요청과 일치하지 않습니다.");
        }
        return transactionService.markSucceeded(
                refundCall.refundId(), refundCall.processingToken(), result.refundTransactionKey());
    }

    private RefundResult callRefund(RefundCall.CancelRequired refundCall, String target) {
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

    private RefundLookupResult lookupRefund(RefundCall.LookupRequired refundCall, String target) {
        try {
            RefundLookupResult result = paymentPort.lookupRefund(
                    refundCall.paymentKey(), refundCall.amount(), refundCall.idempotencyKey());
            return result != null
                    ? result
                    : RefundLookupResult.unavailable(
                            refundCall.paymentKey(), "PG 환불 조회 응답이 비어 있습니다.");
        } catch (Exception e) {
            log.error("환불 조회 예외 [{} refundId={} type={}]",
                    target, refundCall.refundId(), e.getClass().getSimpleName());
            return RefundLookupResult.unavailable(
                    refundCall.paymentKey(), "PG 환불 조회 결과를 확인할 수 없습니다.");
        }
    }
}
