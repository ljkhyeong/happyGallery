package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultPaymentConfirmService implements PaymentConfirmUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaymentConfirmService.class);

    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final PaymentPort paymentPort;
    private final PaymentConfirmTransactionService transactionService;

    public DefaultPaymentConfirmService(PaymentPort paymentPort,
                                        PaymentConfirmTransactionService transactionService) {
        this.paymentPort = paymentPort;
        this.transactionService = transactionService;
    }

    @Override
    public ConfirmResult confirm(ConfirmCommand command) {
        PaymentConfirmTransactionService.ClaimedAttempt claimed = transactionService.claim(command);
        String confirmedPaymentKey = claimed.confirmedPaymentKey();

        if (!claimed.approved()) {
            if (claimed.amount() > 0) {
                PaymentConfirmResult pg = callPayment(claimed);
                if (!pg.success()) {
                    String reason = failureReason(pg.failReason(), "결제 확정에 실패했습니다.");
                    transactionService.recordPgFailure(claimed.id(), reason, pg.retryable());
                    throw new HappyGalleryException(ErrorCode.PAYMENT_FAILED, reason);
                }
                confirmedPaymentKey = pg.paymentKey();
            } else {
                log.debug("amount=0 결제 — PG 호출 생략 [orderId={}]", claimed.orderId());
            }

            try {
                transactionService.markApproved(claimed.id(), confirmedPaymentKey);
            } catch (RuntimeException approvalFailure) {
                compensateAfterApproval(claimed, confirmedPaymentKey, approvalFailure);
                throw approvalFailure;
            }
        }

        try {
            return transactionService.fulfillAndConfirm(claimed.id());
        } catch (RuntimeException fulfillmentFailure) {
            if (claimed.amount() > 0) {
                compensateAfterApproval(claimed, confirmedPaymentKey, fulfillmentFailure);
            } else {
                transactionService.markZeroAmountFulfillmentFailed(
                        claimed.id(), failureReason(fulfillmentFailure.getMessage(), "도메인 생성에 실패했습니다."));
            }
            throw fulfillmentFailure;
        }
    }

    private PaymentConfirmResult callPayment(PaymentConfirmTransactionService.ClaimedAttempt claimed) {
        try {
            PaymentConfirmResult result = paymentPort.confirm(
                    claimed.paymentKey(), claimed.orderId(), claimed.amount(), claimed.idempotencyKey());
            return result != null
                    ? result
                    : PaymentConfirmResult.retryableFailure("PG 응답이 비어 있습니다.");
        } catch (RuntimeException e) {
            log.warn("PG confirm 호출 예외 [orderId={}]", claimed.orderId(), e);
            return PaymentConfirmResult.retryableFailure(
                    failureReason(e.getMessage(), "PG 호출 중 오류가 발생했습니다."));
        }
    }

    private void compensateAfterApproval(PaymentConfirmTransactionService.ClaimedAttempt claimed,
                                         String confirmedPaymentKey,
                                         RuntimeException failure) {
        String reason = failureReason(failure.getMessage(), "PG 승인 후 도메인 생성에 실패했습니다.");
        try {
            transactionService.requestCompensation(claimed.id(), confirmedPaymentKey, reason);
        } catch (RuntimeException compensationFailure) {
            log.error("PG 승인 결제의 보상 환불 요청 저장 실패 [attemptId={}, orderId={}]",
                    claimed.id(), claimed.orderId(), compensationFailure);
        }
    }

    private String failureReason(String reason, String fallback) {
        String resolved = reason == null || reason.isBlank() ? fallback : reason;
        return resolved.length() <= MAX_FAILURE_REASON_LENGTH
                ? resolved
                : resolved.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
