package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Service
class PaymentConfirmTransactionService {

    static final Duration CONFIRM_RECOVERY_DELAY = Duration.ofMinutes(1);
    static final Duration CONFIRM_AUTOMATIC_RETRY_MAX_AGE = Duration.ofDays(14);

    private static final String CONFIRM_RECONCILIATION_REASON =
            "PG 멱등 응답 안전 기간이 지나 결제 상태 대사가 필요합니다.";

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final RefundExecutionService refundExecutionService;
    private final PaymentAttemptAccessVerifier accessVerifier;
    private final CompletedGuestAccessTokenResolver accessTokenResolver;
    private final Map<PaymentContext, PaymentFulfiller> fulfillers;
    private final ObjectMapper objectMapper;
    private final FieldEncryptor fieldEncryptor;
    private final Clock clock;

    PaymentConfirmTransactionService(PaymentAttemptReaderPort attemptReader,
                                     PaymentAttemptStorePort attemptStore,
                                     RefundExecutionService refundExecutionService,
                                     PaymentAttemptAccessVerifier accessVerifier,
                                     CompletedGuestAccessTokenResolver accessTokenResolver,
                                     List<PaymentFulfiller> fulfillers,
                                     ObjectMapper objectMapper,
                                     FieldEncryptor fieldEncryptor,
                                     Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.refundExecutionService = refundExecutionService;
        this.accessVerifier = accessVerifier;
        this.accessTokenResolver = accessTokenResolver;
        this.fulfillers = new EnumMap<>(PaymentContext.class);
        for (PaymentFulfiller fulfiller : fulfillers) {
            PaymentContext context = fulfiller.context();
            if (this.fulfillers.put(context, fulfiller) != null) {
                throw new IllegalStateException("결제 확정 전략이 중복 등록되었습니다: " + context);
            }
        }
        this.objectMapper = objectMapper;
        this.fieldEncryptor = fieldEncryptor;
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
        if (attempt.expirePendingBefore(prepareExpiredBeforeUtc)) {
            attemptStore.save(attempt);
            return new Expired();
        }
        validateAttempt(attempt, command);
        String paymentKey = StringUtils.hasText(command.paymentKey()) ? command.paymentKey() : null;

        if (attempt.getStatus() == PaymentAttemptStatus.CONFIRMED) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            return new Completed(confirmedResult(attempt));
        }
        if (attempt.getStatus() == PaymentAttemptStatus.APPROVED) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            return readyForFulfillment(attempt);
        }
        if (attempt.getStatus() == PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            throw paymentFailure(attempt);
        }
        if (attempt.getStatus() == PaymentAttemptStatus.FAILED) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            throw paymentFailure(attempt);
        }
        if (attempt.requiresConfirmReconciliation(
                LocalDateTime.ofInstant(
                        nowInstant.minus(CONFIRM_AUTOMATIC_RETRY_MAX_AGE), ZoneOffset.UTC))) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            attempt.markConfirmReconciliationRequired(CONFIRM_RECONCILIATION_REASON);
            attemptStore.save(attempt);
            return new ConfirmationRejected(attempt.getId(), paymentFailure(attempt));
        }
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
            return new ZeroAmountApprovalRequired(
                    attempt.getId(), attempt.getOrderIdExternal(), processingToken);
        }
        return new PgConfirmationRequired(
                attempt.getId(), attempt.getOrderIdExternal(), attempt.getAmount(),
                attempt.getPaymentKey(), processingToken);
    }

    /**
     * 배치가 저장된 결제 정보만으로 동일 confirm 요청을 복원한다.
     *
     * <p>후보 목록 조회 이후의 상태 변경을 반영하도록 행 잠금 아래 상태와 제한 시간을 다시 확인한다.
     * 반환 후 경합은 실제 confirm의 실행권 선점과 멱등성 검증이 처리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmRecoveryStep resolveConfirmRecovery(Long attemptId) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        Instant nowInstant = clock.instant();
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, clock.getZone());
        LocalDateTime activityStaleBefore = now.minus(CONFIRM_RECOVERY_DELAY);
        LocalDateTime createdAtStaleBeforeUtc = LocalDateTime.ofInstant(
                nowInstant.minus(CONFIRM_RECOVERY_DELAY), ZoneOffset.UTC);
        if (!attempt.isConfirmRecoveryCandidate(activityStaleBefore, createdAtStaleBeforeUtc)) {
            return new RecoverySkipped();
        }
        attempt.markConfirmRecoveryAttempted(now);
        if (attempt.requiresConfirmReconciliation(LocalDateTime.ofInstant(
                nowInstant.minus(CONFIRM_AUTOMATIC_RETRY_MAX_AGE), ZoneOffset.UTC))) {
            attempt.markConfirmReconciliationRequired(CONFIRM_RECONCILIATION_REASON);
            attemptStore.save(attempt);
            return new ReconciliationRequired();
        }
        attemptStore.save(attempt);
        try {
            PaymentPayload payload = deserialize(attempt.getPayloadEnc());
            AuthContext auth = payload.userId() == null
                    ? AuthContext.guest()
                    : AuthContext.member(payload.userId());
            return new RecoveryReady(ConfirmCommand.trustedRecovery(
                    attempt.getPaymentKey(), attempt.getOrderIdExternal(), attempt.getAmount(), auth));
        } catch (RuntimeException failure) {
            return new RecoveryPreparationFailed(failure);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationStep resolveAfterLostProcessingOwnership(ConfirmCommand command) {
        PaymentAttempt attempt = findValidatedAttemptForUpdate(command);
        String paymentKey = StringUtils.hasText(command.paymentKey()) ? command.paymentKey() : null;
        attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
        return switch (attempt.getStatus()) {
            case CONFIRMED -> new Completed(confirmedResult(attempt));
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
            return new Completed(confirmedResult(attempt));
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
                                      boolean retryable) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        boolean changed = retryable
                ? attempt.markRetryable(processingToken, reason)
                : attempt.markProcessingFailed(processingToken, reason);
        if (!changed) {
            return false;
        }
        attemptStore.save(attempt);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmResult fulfillAndConfirm(Long attemptId) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() == PaymentAttemptStatus.CONFIRMED) {
            return confirmedResult(attempt);
        }
        if (attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "승인된 결제만 도메인 생성에 사용할 수 있습니다.");
        }
        PaymentPayload payload = deserialize(attempt.getPayloadEnc());
        PaymentFulfiller fulfiller = fulfiller(attempt.getContext());
        PaymentFulfiller.FulfillResult fulfilled = fulfiller.fulfill(attempt, payload);
        String accessTokenEnc = fulfilled.rawAccessToken() == null
                ? null
                : fieldEncryptor.encrypt(fulfilled.rawAccessToken());
        attempt.markConfirmed(fulfilled.domainId(), accessTokenEnc);
        attemptStore.save(attempt);
        return confirmedResult(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean requestCompensationForUnpersistedApproval(Long attemptId,
                                                             String processingToken,
                                                             String confirmedPaymentKey,
                                                             String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() != PaymentAttemptStatus.PROCESSING
                || !attempt.markApproved(processingToken, confirmedPaymentKey, LocalDateTime.now(clock))) {
            return false;
        }
        requestCompensation(attempt, confirmedPaymentKey, reason);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean requestCompensationAfterFulfillmentFailure(Long attemptId,
                                                              String confirmedPaymentKey,
                                                              String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
            return false;
        }
        requestCompensation(attempt, confirmedPaymentKey, reason);
        return true;
    }

    private void requestCompensation(PaymentAttempt attempt,
                                     String confirmedPaymentKey,
                                     String reason) {
        requireSameConfirmedPaymentKey(attempt, confirmedPaymentKey);
        attempt.markCompensationRequested(reason);
        attemptStore.save(attempt);
        refundExecutionService.requestPaymentAttemptRefund(
                attempt.getId(), attempt.getAmount(), confirmedPaymentKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkZeroAmountFulfillmentFailed(Long attemptId, String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
            return false;
        }
        attempt.markFailed(reason);
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
        PaymentPayload payload = deserialize(attempt.getPayloadEnc());
        fulfiller(attempt.getContext()).validateStoredPayload(attempt, payload);
        requireSameActor(payload, command);
    }

    private PaymentFulfiller fulfiller(PaymentContext context) {
        PaymentFulfiller fulfiller = fulfillers.get(context);
        if (fulfiller == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 컨텍스트입니다.");
        }
        return fulfiller;
    }

    private void requireAccess(PaymentAttempt attempt, ConfirmCommand command) {
        if (!command.trustedInternalRecovery()) {
            accessVerifier.requireCustomerAccess(attempt, command.auth(), command.statusToken());
        }
    }

    private void requireSameActor(PaymentPayload payload, ConfirmCommand command) {
        if (!Objects.equals(payload.userId(), command.auth().userId())) {
            if (!command.trustedInternalRecovery()) {
                throw new NotFoundException("결제");
            }
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "복구할 결제의 사용자 정보가 저장값과 일치하지 않습니다.");
        }
    }

    private PaymentPayload deserialize(String storedPayload) {
        String json = fieldEncryptor.decrypt(storedPayload);
        return objectMapper.readValue(json, PaymentPayload.class);
    }

    private ConfirmResult confirmedResult(PaymentAttempt attempt) {
        if (attempt.getFulfilledDomainId() == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "완료된 결제 결과가 없습니다.");
        }
        CompletedGuestAccessTokenResolver.ResolvedAccess access = accessTokenResolver.resolve(attempt);
        return new ConfirmResult(
                attempt.getContext(),
                attempt.getFulfilledDomainId(),
                access.accessToken(),
                access.recoveryRequired());
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

    sealed interface ConfirmRecoveryStep
            permits RecoverySkipped, ReconciliationRequired, RecoveryReady, RecoveryPreparationFailed {}

    record RecoverySkipped() implements ConfirmRecoveryStep {}

    record ReconciliationRequired() implements ConfirmRecoveryStep {}

    record RecoveryReady(ConfirmCommand command) implements ConfirmRecoveryStep {}

    record RecoveryPreparationFailed(RuntimeException failure) implements ConfirmRecoveryStep {}

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
                                  String processingToken) implements ConfirmationStep {
        String idempotencyKey() {
            return orderId;
        }
    }

    record ZeroAmountApprovalRequired(Long attemptId,
                                      String orderId,
                                      String processingToken) implements ConfirmationStep {}
}
