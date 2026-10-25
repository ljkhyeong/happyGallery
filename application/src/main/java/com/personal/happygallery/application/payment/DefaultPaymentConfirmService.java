package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.monitoring.AppMetrics;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.Completed;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.ConfirmationRejected;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.ConfirmationStep;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.Expired;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.PgConfirmationRequired;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.ReadyForFulfillment;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.ZeroAmountApprovalRequired;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.text.DomainText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static java.util.Objects.requireNonNull;

@Service
public class DefaultPaymentConfirmService implements PaymentConfirmUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaymentConfirmService.class);

    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final PaymentPort paymentPort;
    private final PaymentConfirmClaimTransactionService claimTransactionService;
    private final PaymentConfirmFulfillmentTransactionService fulfillmentTransactionService;
    private final PaymentFulfillmentCoordinator fulfillmentCoordinator;
    private final AppMetrics appMetrics;

    public DefaultPaymentConfirmService(PaymentPort paymentPort,
                                        PaymentConfirmClaimTransactionService claimTransactionService,
                                        PaymentConfirmFulfillmentTransactionService fulfillmentTransactionService,
                                        PaymentFulfillmentCoordinator fulfillmentCoordinator,
                                        AppMetrics appMetrics) {
        this.paymentPort = paymentPort;
        this.claimTransactionService = claimTransactionService;
        this.fulfillmentTransactionService = fulfillmentTransactionService;
        this.fulfillmentCoordinator = fulfillmentCoordinator;
        this.appMetrics = appMetrics;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public ConfirmResult confirm(ConfirmCommand command) {
        ConfirmationStep step = claimTransactionService.resolveConfirmationStep(command);
        while (true) {
            switch (step) {
                case Completed completed -> {
                    return completed.result();
                }
                case ConfirmationRejected rejected -> {
                    appMetrics.incrementPaymentConfirmReconciliationRequired();
                    log.error("결제 확정 자동 재확인 안전 기간 초과 — 수동 대사 필요 "
                                    + "[attemptId={}, orderId={}]",
                            rejected.attemptId(), command.orderId());
                    throw rejected.failure();
                }
                case Expired ignored ->
                        throw new HappyGalleryException(ErrorCode.PAYMENT_ATTEMPT_EXPIRED);
                case ReadyForFulfillment ready -> {
                    return fulfill(ready);
                }
                case ZeroAmountApprovalRequired required -> {
                    log.debug("amount=0 결제 — PG 호출 생략 [orderId={}]", required.orderId());
                    if (claimTransactionService.tryMarkApproved(
                            required.attemptId(), required.processingToken(), null, null, null)) {
                        return fulfill(new ReadyForFulfillment(
                                required.attemptId(), required.orderId(), 0L, null));
                    }
                    step = claimTransactionService.resolveAfterLostProcessingOwnership(command);
                }
                case PgConfirmationRequired required -> {
                    PaymentConfirmResult pg = callPayment(required);
                    if (!pg.success()) {
                        String reason = failureReason(pg.failReason(), "결제 확정에 실패했습니다.");
                        if (pg.reconciliationRequired()
                                || (!pg.retryable() && required.priorPgCallPossible())) {
                            if (claimTransactionService.tryRecordPgReconciliationRequired(
                                    required.attemptId(), required.processingToken(), reason)) {
                                appMetrics.incrementPaymentConfirmReconciliationRequired();
                                throw new HappyGalleryException(
                                        ErrorCode.PAYMENT_RECONCILIATION_REQUIRED, reason);
                            }
                            step = claimTransactionService.resolveAfterLostProcessingOwnership(command);
                            continue;
                        }
                        if (claimTransactionService.tryRecordPgFailure(
                                required.attemptId(), required.processingToken(), reason,
                                pg.retryable(), required.priorPgCallPossible())) {
                            ErrorCode errorCode = pg.retryable()
                                    ? ErrorCode.PAYMENT_CONFIRM_RETRYABLE
                                    : ErrorCode.PAYMENT_FAILED;
                            throw new HappyGalleryException(errorCode, reason);
                        }
                        step = claimTransactionService.resolveAfterLostProcessingOwnership(command);
                        continue;
                    }

                    String confirmedPaymentKey = pg.paymentKey();
                    String confirmedPaymentMethod = pg.method();
                    boolean approved;
                    try {
                        approved = claimTransactionService.tryMarkApproved(
                                required.attemptId(), required.processingToken(),
                                confirmedPaymentKey, confirmedPaymentMethod, pg.receiptUrl());
                    } catch (RuntimeException approvalFailure) {
                        compensateUnpersistedApproval(
                                required, confirmedPaymentKey, confirmedPaymentMethod, approvalFailure);
                        throw approvalFailure;
                    }
                    if (approved) {
                        return fulfill(new ReadyForFulfillment(
                                required.attemptId(), required.orderId(), required.amount(), confirmedPaymentKey));
                    }
                    step = claimTransactionService.reconcileLatePgApproval(
                            command, confirmedPaymentKey, confirmedPaymentMethod, pg.receiptUrl());
                }
            }
        }
    }

    private ConfirmResult fulfill(ReadyForFulfillment ready) {
        return fulfillmentCoordinator.fulfillOrCompensate(
                ready.attemptId(), ready.orderId(), ready.amount(), ready.confirmedPaymentKey(),
                "PG 승인 후 도메인 생성에 실패했습니다.");
    }

    private PaymentConfirmResult callPayment(PgConfirmationRequired required) {
        return requireNonNull(
                paymentPort.confirm(
                        required.paymentKey(), required.orderId(), required.amount(), required.idempotencyKey()),
                "PaymentPort.confirm은 null을 반환할 수 없습니다.");
    }

    private void compensateUnpersistedApproval(
            PgConfirmationRequired required,
            String confirmedPaymentKey,
            String confirmedPaymentMethod,
            RuntimeException originalFailure) {
        try {
            boolean requested = fulfillmentTransactionService.requestCompensationForUnpersistedApproval(
                    required.attemptId(), required.processingToken(), confirmedPaymentKey,
                    confirmedPaymentMethod,
                    "PG 승인 후 결제 상태 저장에 실패했습니다.");
            if (!requested) {
                log.warn("stale confirm 결과의 보상 환불 요청을 건너뜁니다 [attemptId={}, orderId={}]",
                        required.attemptId(), required.orderId());
            }
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
            log.error("PG 승인 결제의 보상 환불 요청 저장 실패 — 자동 복구 대상 유지 "
                            + "[attemptId={}, orderId={}, type={}]",
                    required.attemptId(), required.orderId(), compensationFailure.getClass().getSimpleName());
        }
    }

    private String failureReason(String reason, String fallback) {
        String resolved = StringUtils.hasText(reason) ? reason : fallback;
        return DomainText.truncate(resolved, MAX_FAILURE_REASON_LENGTH);
    }
}
