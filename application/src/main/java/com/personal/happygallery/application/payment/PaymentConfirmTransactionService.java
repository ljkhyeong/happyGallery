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
import java.time.LocalDateTime;
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

    private static final Duration PROCESSING_STALE_AFTER = Duration.ofMinutes(1);

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final RefundExecutionService refundExecutionService;
    private final Map<PaymentContext, PaymentFulfiller> fulfillers;
    private final ObjectMapper objectMapper;
    private final FieldEncryptor fieldEncryptor;
    private final Clock clock;

    PaymentConfirmTransactionService(PaymentAttemptReaderPort attemptReader,
                                     PaymentAttemptStorePort attemptStore,
                                     RefundExecutionService refundExecutionService,
                                     List<PaymentFulfiller> fulfillers,
                                     ObjectMapper objectMapper,
                                     FieldEncryptor fieldEncryptor,
                                     Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.refundExecutionService = refundExecutionService;
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
        PaymentAttempt attempt = findValidatedAttemptForUpdate(command);
        String paymentKey = StringUtils.hasText(command.paymentKey()) ? command.paymentKey() : null;

        LocalDateTime now = LocalDateTime.now(clock);
        if (attempt.getStatus() == PaymentAttemptStatus.CONFIRMED) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            return new Completed(confirmedResult(attempt));
        }
        if (attempt.getStatus() == PaymentAttemptStatus.APPROVED) {
            attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
            return readyForFulfillment(attempt);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmationStep resolveAfterLostProcessingOwnership(ConfirmCommand command) {
        PaymentAttempt attempt = findValidatedAttemptForUpdate(command);
        String paymentKey = StringUtils.hasText(command.paymentKey()) ? command.paymentKey() : null;
        attempt.requireMatchingConfirmRequest(command.amount(), paymentKey);
        if (attempt.getStatus() == PaymentAttemptStatus.CONFIRMED) {
            return new Completed(confirmedResult(attempt));
        }
        if (attempt.getStatus() == PaymentAttemptStatus.APPROVED) {
            return readyForFulfillment(attempt);
        }
        throw new HappyGalleryException(ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS);
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
        PaymentFulfiller.FulfillResult fulfilled = fulfiller.fulfill(payload, attempt.getConfirmedPaymentKey());
        String accessTokenEnc = fulfilled.rawAccessToken() == null
                ? null
                : fieldEncryptor.encrypt(fulfilled.rawAccessToken());
        attempt.markConfirmed(fulfilled.domainId(), accessTokenEnc);
        attemptStore.save(attempt);
        return new ConfirmResult(attempt.getContext(), fulfilled.domainId(), fulfilled.rawAccessToken());
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
        PaymentAttempt attempt = attemptReader.findByOrderIdExternalForUpdate(command.orderId())
                .orElseThrow(() -> new NotFoundException("결제 시도"));
        PaymentPayload payload = deserialize(attempt.getPayloadEnc());
        fulfiller(attempt.getContext()).validateStoredPayload(attempt, payload);
        requireSameActor(payload, command.auth());
        return attempt;
    }

    private PaymentFulfiller fulfiller(PaymentContext context) {
        PaymentFulfiller fulfiller = fulfillers.get(context);
        if (fulfiller == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 컨텍스트입니다.");
        }
        return fulfiller;
    }

    private void requireSameActor(PaymentPayload payload, AuthContext auth) {
        if (!Objects.equals(payload.userId(), auth.userId())) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "결제를 준비한 사용자와 현재 인증 정보가 일치하지 않습니다.");
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
        String accessToken = attempt.getFulfilledAccessTokenEnc() == null
                ? null
                : fieldEncryptor.decrypt(attempt.getFulfilledAccessTokenEnc());
        return new ConfirmResult(attempt.getContext(), attempt.getFulfilledDomainId(), accessToken);
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

    private boolean isStale(PaymentAttempt attempt, LocalDateTime now) {
        return attempt.getProcessingAt() == null
                || !attempt.getProcessingAt().isAfter(now.minus(PROCESSING_STALE_AFTER));
    }

    sealed interface ConfirmationStep
            permits Completed, ReadyForFulfillment, PgConfirmationRequired, ZeroAmountApprovalRequired {}

    record Completed(ConfirmResult result) implements ConfirmationStep {}

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
