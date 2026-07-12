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
    private static final String MISSING_PAYMENT_KEY_REASON = "paymentKey가 없어 PG 환불을 실행할 수 없습니다.";

    private final PaymentPort paymentPort;
    private final RefundTransactionService transactionService;

    RefundDispatcher(PaymentPort paymentPort, RefundTransactionService transactionService) {
        this.paymentPort = paymentPort;
        this.transactionService = transactionService;
    }

    @Transactional(propagation = Propagation.NEVER)
    public Refund dispatch(Long refundId, String target) {
        RefundCall refundCall = transactionService.prepareRefundCall(refundId, MISSING_PAYMENT_KEY_REASON);
        if (refundCall.failedBeforePgCall()) {
            log.warn("환불 실패 [{} refundId={}] reason=paymentKey 없음", target, refundId);
            return refundCall.failedRefund();
        }

        RefundResult result = callPayment(refundCall, target);
        if (result.success()) {
            return transactionService.markSucceeded(refundId, result.refundTransactionKey());
        }
        log.warn("환불 실패 [{} refundId={}] reason={}", target, refundId, result.failReason());
        return transactionService.markFailed(refundId, result.failReason());
    }

    private RefundResult callPayment(RefundCall refundCall, String target) {
        try {
            RefundResult result = paymentPort.refund(
                    refundCall.paymentKey(), refundCall.amount(), refundCall.idempotencyKey());
            return result != null ? result : RefundResult.failure("PG 응답이 비어 있습니다.");
        } catch (Exception e) {
            log.error("환불 호출 예외 [{} refundId={}]", target, refundCall.refundId(), e);
            return RefundResult.failure(e.getMessage());
        }
    }
}
