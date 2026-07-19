package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    @Transactional(propagation = Propagation.NEVER)
    public ConfirmResult confirm(ConfirmCommand command) {
        PaymentConfirmTransactionService.ConfirmationStep step =
                transactionService.resolveConfirmationStep(command);
        while (true) {
            switch (step) {
                case PaymentConfirmTransactionService.Completed completed -> {
                    return completed.result();
                }
                case PaymentConfirmTransactionService.ReadyForFulfillment ready -> {
                    return fulfill(ready);
                }
                case PaymentConfirmTransactionService.ZeroAmountApprovalRequired required -> {
                    log.debug("amount=0 결제 — PG 호출 생략 [orderId={}]", required.orderId());
                    if (transactionService.tryMarkApproved(
                            required.attemptId(), required.processingToken(), null)) {
                        return fulfill(new PaymentConfirmTransactionService.ReadyForFulfillment(
                                required.attemptId(), required.orderId(), 0L, null));
                    }
                    step = transactionService.resolveAfterLostProcessingOwnership(command);
                }
                case PaymentConfirmTransactionService.PgConfirmationRequired required -> {
                    PaymentConfirmResult pg = callPayment(required);
                    if (!pg.success()) {
                        String reason = failureReason(pg.failReason(), "결제 확정에 실패했습니다.");
                        if (transactionService.tryRecordPgFailure(
                                required.attemptId(), required.processingToken(), reason, pg.retryable())) {
                            throw new HappyGalleryException(ErrorCode.PAYMENT_FAILED, reason);
                        }
                        step = transactionService.resolveAfterLostProcessingOwnership(command);
                        continue;
                    }

                    String confirmedPaymentKey = pg.paymentKey();
                    boolean approved;
                    try {
                        approved = transactionService.tryMarkApproved(
                                required.attemptId(), required.processingToken(), confirmedPaymentKey);
                    } catch (RuntimeException approvalFailure) {
                        compensateUnpersistedApproval(required, confirmedPaymentKey, approvalFailure);
                        throw approvalFailure;
                    }
                    if (approved) {
                        return fulfill(new PaymentConfirmTransactionService.ReadyForFulfillment(
                                required.attemptId(), required.orderId(), required.amount(), confirmedPaymentKey));
                    }
                    step = transactionService.resolveAfterLostProcessingOwnership(command);
                }
            }
        }
    }

    private ConfirmResult fulfill(PaymentConfirmTransactionService.ReadyForFulfillment ready) {
        try {
            return transactionService.fulfillAndConfirm(ready.attemptId());
        } catch (RuntimeException fulfillmentFailure) {
            if (ready.amount() > 0L) {
                compensateAfterFulfillmentFailure(ready, fulfillmentFailure);
            } else {
                recordZeroAmountFailure(ready, fulfillmentFailure);
            }
            throw fulfillmentFailure;
        }
    }

    private PaymentConfirmResult callPayment(PaymentConfirmTransactionService.PgConfirmationRequired required) {
        try {
            PaymentConfirmResult result = paymentPort.confirm(
                    required.paymentKey(), required.orderId(), required.amount(), required.idempotencyKey());
            return result != null
                    ? result
                    : PaymentConfirmResult.retryableFailure("PG 응답이 비어 있습니다.");
        } catch (RuntimeException e) {
            log.warn("PG confirm 호출 예외 [orderId={}, type={}]",
                    required.orderId(), e.getClass().getSimpleName());
            return PaymentConfirmResult.retryableFailure("PG 호출 중 오류가 발생했습니다.");
        }
    }

    private void compensateUnpersistedApproval(
            PaymentConfirmTransactionService.PgConfirmationRequired required,
            String confirmedPaymentKey,
            RuntimeException originalFailure) {
        try {
            boolean requested = transactionService.requestCompensationForUnpersistedApproval(
                    required.attemptId(), required.processingToken(), confirmedPaymentKey,
                    "PG 승인 후 결제 상태 저장에 실패했습니다.");
            if (!requested) {
                log.warn("stale confirm 결과의 보상 환불 요청을 건너뜁니다 [attemptId={}, orderId={}]",
                        required.attemptId(), required.orderId());
            }
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
            log.error("PG 승인 결제의 보상 환불 요청 저장 실패 [attemptId={}, orderId={}, type={}]",
                    required.attemptId(), required.orderId(), compensationFailure.getClass().getSimpleName());
        }
    }

    private void compensateAfterFulfillmentFailure(
            PaymentConfirmTransactionService.ReadyForFulfillment ready,
            RuntimeException originalFailure) {
        try {
            boolean requested = transactionService.requestCompensationAfterFulfillmentFailure(
                    ready.attemptId(), ready.confirmedPaymentKey(),
                    "PG 승인 후 도메인 생성에 실패했습니다.");
            if (!requested) {
                log.warn("완료되었거나 다른 요청이 처리한 결제의 보상 환불을 건너뜁니다 [attemptId={}, orderId={}]",
                        ready.attemptId(), ready.orderId());
            }
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
            log.error("PG 승인 결제의 보상 환불 요청 저장 실패 [attemptId={}, orderId={}, type={}]",
                    ready.attemptId(), ready.orderId(), compensationFailure.getClass().getSimpleName());
        }
    }

    private void recordZeroAmountFailure(PaymentConfirmTransactionService.ReadyForFulfillment ready,
                                         RuntimeException originalFailure) {
        try {
            boolean recorded = transactionService.tryMarkZeroAmountFulfillmentFailed(
                    ready.attemptId(), "도메인 생성에 실패했습니다.");
            if (!recorded) {
                log.warn("stale amount=0 결제 실패 결과를 건너뜁니다 [attemptId={}, orderId={}]",
                        ready.attemptId(), ready.orderId());
            }
        } catch (RuntimeException stateFailure) {
            originalFailure.addSuppressed(stateFailure);
            log.error("amount=0 결제 실패 상태 저장 실패 [attemptId={}, orderId={}, type={}]",
                    ready.attemptId(), ready.orderId(), stateFailure.getClass().getSimpleName());
        }
    }

    private String failureReason(String reason, String fallback) {
        String resolved = StringUtils.hasText(reason) ? reason : fallback;
        return resolved.length() <= MAX_FAILURE_REASON_LENGTH
                ? resolved
                : resolved.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
