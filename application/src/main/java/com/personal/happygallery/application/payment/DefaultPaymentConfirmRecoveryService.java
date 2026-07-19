package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.monitoring.AppMetrics;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmRecoveryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultPaymentConfirmRecoveryService implements PaymentConfirmRecoveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaymentConfirmRecoveryService.class);

    private static final int BATCH_SIZE = 10;

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentConfirmTransactionService transactionService;
    private final PaymentConfirmUseCase paymentConfirmUseCase;
    private final AppMetrics appMetrics;
    private final Clock clock;

    public DefaultPaymentConfirmRecoveryService(PaymentAttemptReaderPort attemptReader,
                                                PaymentConfirmTransactionService transactionService,
                                                PaymentConfirmUseCase paymentConfirmUseCase,
                                                AppMetrics appMetrics,
                                                Clock clock) {
        this.attemptReader = attemptReader;
        this.transactionService = transactionService;
        this.paymentConfirmUseCase = paymentConfirmUseCase;
        this.appMetrics = appMetrics;
        this.clock = clock;
    }

    @Override
    public BatchResult recoverIncompleteConfirms() {
        Instant staleBefore = clock.instant()
                .minus(PaymentConfirmTransactionService.CONFIRM_RECOVERY_DELAY);
        LocalDateTime activityStaleBefore = LocalDateTime.ofInstant(staleBefore, clock.getZone());
        LocalDateTime createdAtStaleBeforeUtc = LocalDateTime.ofInstant(staleBefore, ZoneOffset.UTC);
        List<Long> attemptIds = attemptReader.findConfirmRecoveryCandidateIds(
                activityStaleBefore, createdAtStaleBeforeUtc, BATCH_SIZE);
        return BatchExecutor.execute(
                attemptIds,
                attemptId -> attemptId,
                this::recover,
                "결제 확정 복구");
    }

    private boolean recover(Long attemptId) {
        return switch (transactionService.resolveConfirmRecovery(attemptId)) {
            case PaymentConfirmTransactionService.RecoverySkipped ignored -> false;
            case PaymentConfirmTransactionService.ReconciliationRequired ignored -> {
                appMetrics.incrementPaymentConfirmReconciliationRequired();
                log.error("결제 확정 자동 재확인 안전 기간 초과 — 수동 대사 필요 [attemptId={}]", attemptId);
                yield true;
            }
            case PaymentConfirmTransactionService.RecoveryReady ready -> confirmIfAvailable(ready.command());
            case PaymentConfirmTransactionService.RecoveryPreparationFailed failed -> throw failed.failure();
        };
    }

    private boolean confirmIfAvailable(PaymentConfirmUseCase.ConfirmCommand command) {
        try {
            paymentConfirmUseCase.confirm(command);
            return true;
        } catch (HappyGalleryException exception) {
            if (exception.getErrorCode() == ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS) {
                return false;
            }
            throw exception;
        }
    }
}
