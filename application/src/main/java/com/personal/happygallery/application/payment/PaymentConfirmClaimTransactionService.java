package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class PaymentConfirmClaimTransactionService {

    static final Duration CONFIRM_RECOVERY_DELAY = Duration.ofMinutes(1);
    static final Duration CONFIRM_AUTOMATIC_RETRY_MAX_AGE = Duration.ofDays(14);
    static final String CONFIRM_RECONCILIATION_REASON =
            "PG 멱등 응답 안전 기간이 지나 결제 상태 대사가 필요합니다.";

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final PaymentAttemptAccessVerifier accessVerifier;
    private final PaymentConfirmAttemptResolver attemptResolver;
    private final OrderPaymentBenefitReservationService benefitReservationService;
    private final Clock clock;

    PaymentConfirmClaimTransactionService(PaymentAttemptReaderPort attemptReader,
                                          PaymentAttemptStorePort attemptStore,
                                          PaymentAttemptAccessVerifier accessVerifier,
                                          PaymentConfirmAttemptResolver attemptResolver,
                                          OrderPaymentBenefitReservationService benefitReservationService,
                                          Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.accessVerifier = accessVerifier;
        this.attemptResolver = attemptResolver;
        this.benefitReservationService = benefitReservationService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationStep resolveConfirmationStep(ConfirmCommand command) {
        PaymentAttempt attempt = findForUpdate(command.orderId());
        requireAccess(attempt, command);
        if (attempt.getStatus() == PaymentAttemptStatus.CANCELED) {
            throw new HappyGalleryException(ErrorCode.PAYMENT_ATTEMPT_EXPIRED);
        }
        Instant nowInstant = clock.instant();
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, clock.getZone());
        LocalDateTime prepareExpiredBeforeUtc = LocalDateTime.ofInstant(
                nowInstant.minus(DefaultPaymentAttemptExpiryBatchService.PREPARE_TTL), ZoneOffset.UTC);
        PreparedPaymentPayload expiryPayload = attempt.getStatus() == PaymentAttemptStatus.PENDING
                ? benefitReservationService.readPayloadForRelease(attempt)
                : null;
        if (attempt.expirePendingBefore(prepareExpiredBeforeUtc)) {
            attemptStore.save(attempt);
            if (expiryPayload != null) {
                benefitReservationService.release(attempt, expiryPayload, now);
            }
            return new Expired();
        }
        validateAttempt(attempt, command);
        String paymentKey = StringUtils.hasText(command.paymentKey()) ? command.paymentKey() : null;

        if (attempt.requiresConfirmReconciliation(
                LocalDateTime.ofInstant(
                        nowInstant.minus(CONFIRM_AUTOMATIC_RETRY_MAX_AGE), ZoneOffset.UTC))) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            attempt.markConfirmReconciliationRequired(CONFIRM_RECONCILIATION_REASON);
            attemptStore.save(attempt);
            return new ConfirmationRejected(attempt.getId(), paymentFailure(attempt));
        }

        return switch (attempt.getStatus()) {
            case CONFIRMED -> {
                attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
                yield new Completed(attemptResolver.confirmedResult(attempt));
            }
            case APPROVED -> {
                attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
                yield readyForFulfillment(attempt);
            }
            case RECONCILIATION_REQUIRED, FAILED -> {
                attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
                throw paymentFailure(attempt);
            }
            case PENDING, PROCESSING, RETRYABLE -> {
                boolean priorPgCallPossible = attempt.getStatus() != PaymentAttemptStatus.PENDING;
                String processingToken;
                if (attempt.getStatus() == PaymentAttemptStatus.PROCESSING) {
                    if (!isStale(attempt, now)) {
                        attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
                        throw new HappyGalleryException(ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS);
                    }
                    processingToken = attempt.restartProcessing(command.amount(), paymentKey, now);
                } else {
                    processingToken = attempt.startProcessing(command.amount(), paymentKey, now);
                }
                attemptStore.save(attempt);
                if (attempt.getAmount() == 0L) {
                    yield new ZeroAmountApprovalRequired(
                            attempt.getId(), attempt.getOrderIdExternal(), processingToken);
                }
                yield new PgConfirmationRequired(
                        attempt.getId(), attempt.getOrderIdExternal(), attempt.getAmount(),
                        attempt.getPaymentKey(), processingToken, priorPgCallPossible);
            }
            case COMPENSATION_REQUESTED, COMPENSATION_FAILED, COMPENSATED ->
                    throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 처리된 결제입니다.");
            case CANCELED -> throw new HappyGalleryException(ErrorCode.PAYMENT_ATTEMPT_EXPIRED);
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationStep resolveAfterLostProcessingOwnership(ConfirmCommand command) {
        PaymentAttempt attempt = findValidatedAttemptForUpdate(command);
        String paymentKey = StringUtils.hasText(command.paymentKey()) ? command.paymentKey() : null;
        attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
        return switch (attempt.getStatus()) {
            case CONFIRMED -> new Completed(attemptResolver.confirmedResult(attempt));
            case APPROVED -> readyForFulfillment(attempt);
            case RETRYABLE, FAILED, RECONCILIATION_REQUIRED -> throw paymentFailure(attempt);
            case PENDING, PROCESSING ->
                    throw new HappyGalleryException(ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS);
            case COMPENSATION_REQUESTED, COMPENSATION_FAILED, COMPENSATED, CANCELED ->
                    throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 후속 처리된 결제입니다.");
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationStep reconcileLatePgApproval(ConfirmCommand command,
                                                     String confirmedPaymentKey) {
        PaymentAttempt attempt = findValidatedAttemptForUpdate(command);
        String paymentKey = StringUtils.hasText(command.paymentKey()) ? command.paymentKey() : null;
        attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
        if (attempt.getStatus() == PaymentAttemptStatus.CONFIRMED) {
            requireSameConfirmedPaymentKey(attempt, confirmedPaymentKey);
            return new Completed(attemptResolver.confirmedResult(attempt));
        }
        if (attempt.getStatus() == PaymentAttemptStatus.APPROVED) {
            requireSameConfirmedPaymentKey(attempt, confirmedPaymentKey);
            return readyForFulfillment(attempt);
        }
        if (!attempt.reconcileLatePgApproval(confirmedPaymentKey, LocalDateTime.now(clock))) {
            throw new HappyGalleryException(ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS);
        }
        attemptStore.save(attempt);
        return readyForFulfillment(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkApproved(Long attemptId,
                                   String processingToken,
                                   String confirmedPaymentKey) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() == PaymentAttemptStatus.APPROVED
                || attempt.getStatus() == PaymentAttemptStatus.CONFIRMED) {
            requireSameConfirmedPaymentKey(attempt, confirmedPaymentKey);
            return true;
        }
        if (!attempt.markApproved(processingToken, confirmedPaymentKey, LocalDateTime.now(clock))) {
            return false;
        }
        attemptStore.save(attempt);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryRecordPgFailure(Long attemptId,
                                      String processingToken,
                                      String reason,
                                      boolean retryable,
                                      boolean priorPgCallPossible) {
        if (!retryable && priorPgCallPossible) {
            throw new IllegalArgumentException(
                    "이전 PG 호출 가능성이 있는 최종 실패는 대사 확인으로 종결해야 합니다.");
        }
        PaymentAttempt attempt = findForUpdate(attemptId);
        boolean changed = retryable
                ? attempt.markRetryable(processingToken, reason)
                : attempt.markProcessingFailed(processingToken, reason);
        if (!changed) {
            return false;
        }
        attemptStore.save(attempt);
        if (!retryable) {
            benefitReservationService.release(attempt, LocalDateTime.now(clock));
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryRecordPgReconciliationRequired(Long attemptId,
                                                     String processingToken,
                                                     String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (!attempt.markConfirmReconciliationRequired(processingToken, reason)) {
            return false;
        }
        attemptStore.save(attempt);
        return true;
    }

    private PaymentAttempt findForUpdate(Long attemptId) {
        return attemptReader.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new NotFoundException("결제 시도"));
    }

    private PaymentAttempt findValidatedAttemptForUpdate(ConfirmCommand command) {
        PaymentAttempt attempt = findForUpdate(command.orderId());
        requireAccess(attempt, command);
        if (attempt.getStatus() == PaymentAttemptStatus.CANCELED) {
            throw new HappyGalleryException(ErrorCode.PAYMENT_ATTEMPT_EXPIRED);
        }
        validateAttempt(attempt, command);
        return attempt;
    }

    private PaymentAttempt findForUpdate(String orderId) {
        return attemptReader.findByOrderIdExternalForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("결제"));
    }

    private void validateAttempt(PaymentAttempt attempt, ConfirmCommand command) {
        if (attempt.getPayloadEnc() == null) {
            if (attempt.getStatus() == PaymentAttemptStatus.FAILED) {
                throw paymentFailure(attempt);
            }
            throw new HappyGalleryException(ErrorCode.PAYMENT_RESULT_RETENTION_EXPIRED);
        }
        PreparedPaymentPayload payload = attemptResolver.readPayload(attempt);
        attemptResolver.validateStoredPayload(attempt, payload);
        requireSameActor(payload, command);
    }

    private void requireAccess(PaymentAttempt attempt, ConfirmCommand command) {
        if (!command.trustedInternalRecovery()) {
            accessVerifier.requireCustomerAccess(attempt, command.auth(), command.statusToken());
        }
    }

    private void requireSameActor(PreparedPaymentPayload payload, ConfirmCommand command) {
        if (!Objects.equals(payload.userId(), command.auth().userId())) {
            if (!command.trustedInternalRecovery()) {
                throw new NotFoundException("결제");
            }
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "복구할 결제의 사용자 정보가 저장값과 일치하지 않습니다.");
        }
    }

    private ReadyForFulfillment readyForFulfillment(PaymentAttempt attempt) {
        return new ReadyForFulfillment(
                attempt.getId(), attempt.getOrderIdExternal(), attempt.getAmount(),
                attempt.getConfirmedPaymentKey());
    }

    private void requireSameConfirmedPaymentKey(PaymentAttempt attempt, String confirmedPaymentKey) {
        if (!Objects.equals(attempt.getConfirmedPaymentKey(), confirmedPaymentKey)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "PG 결제 키가 기존 승인 결과와 일치하지 않습니다.");
        }
    }

    private HappyGalleryException paymentFailure(PaymentAttempt attempt) {
        ErrorCode errorCode = switch (attempt.getStatus()) {
            case RETRYABLE -> ErrorCode.PAYMENT_CONFIRM_RETRYABLE;
            case RECONCILIATION_REQUIRED -> ErrorCode.PAYMENT_RECONCILIATION_REQUIRED;
            default -> ErrorCode.PAYMENT_FAILED;
        };
        String reason = StringUtils.hasText(attempt.getFailReason())
                ? attempt.getFailReason()
                : errorCode.message;
        return new HappyGalleryException(errorCode, reason);
    }

    private boolean isStale(PaymentAttempt attempt, LocalDateTime now) {
        return attempt.getProcessingAt() == null
                || !attempt.getProcessingAt().isAfter(now.minus(CONFIRM_RECOVERY_DELAY));
    }

    sealed interface ConfirmationStep
            permits Completed, ConfirmationRejected, ReadyForFulfillment,
                    PgConfirmationRequired, ZeroAmountApprovalRequired, Expired {}

    record Completed(ConfirmResult result) implements ConfirmationStep {}

    record ConfirmationRejected(Long attemptId,
                                HappyGalleryException failure) implements ConfirmationStep {}

    record Expired() implements ConfirmationStep {}

    record ReadyForFulfillment(Long attemptId,
                               String orderId,
                               long amount,
                               String confirmedPaymentKey) implements ConfirmationStep {}

    record PgConfirmationRequired(Long attemptId,
                                  String orderId,
                                  long amount,
                                  String paymentKey,
                                  String processingToken,
                                  boolean priorPgCallPossible) implements ConfirmationStep {

        PgConfirmationRequired(Long attemptId,
                               String orderId,
                               long amount,
                               String paymentKey,
                               String processingToken) {
            this(attemptId, orderId, amount, paymentKey, processingToken, false);
        }

        String idempotencyKey() {
            return orderId;
        }
    }

    record ZeroAmountApprovalRequired(Long attemptId,
                                      String orderId,
                                      String processingToken) implements ConfirmationStep {}
}
