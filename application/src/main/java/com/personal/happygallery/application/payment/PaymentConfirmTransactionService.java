package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
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
import tools.jackson.databind.ObjectMapper;

@Service
class PaymentConfirmTransactionService {

    private static final Duration PROCESSING_STALE_AFTER = Duration.ofMinutes(1);

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final RefundExecutionService refundExecutionService;
    private final Map<PaymentContext, PaymentFulfiller> fulfillers;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    PaymentConfirmTransactionService(PaymentAttemptReaderPort attemptReader,
                                     PaymentAttemptStorePort attemptStore,
                                     RefundExecutionService refundExecutionService,
                                     List<PaymentFulfiller> fulfillers,
                                     ObjectMapper objectMapper,
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
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimedAttempt claim(ConfirmCommand command) {
        PaymentAttempt attempt = attemptReader.findByOrderIdExternalForUpdate(command.orderId())
                .orElseThrow(() -> new NotFoundException("결제 시도"));
        String paymentKey = normalize(command.paymentKey());
        PaymentFulfiller fulfiller = fulfiller(attempt.getContext());
        PaymentPayload payload = deserialize(attempt.getPayloadJson());
        fulfiller.validateBeforePg(attempt, payload);
        requireSameActor(payload, command.auth());

        LocalDateTime now = LocalDateTime.now(clock);
        if (attempt.getStatus() == PaymentAttemptStatus.APPROVED) {
            attempt.requireMatchingRequest(command.amount(), paymentKey);
            return ClaimedAttempt.approved(attempt);
        }
        if (attempt.getStatus() == PaymentAttemptStatus.PROCESSING) {
            attempt.requireMatchingRequest(command.amount(), paymentKey);
            if (!isStale(attempt, now)) {
                throw new HappyGalleryException(ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS);
            }
            attempt.restartProcessing(paymentKey, now);
        } else {
            attempt.startProcessing(command.amount(), paymentKey, now);
        }
        attemptStore.save(attempt);
        return ClaimedAttempt.processing(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markApproved(Long attemptId, String confirmedPaymentKey) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() == PaymentAttemptStatus.APPROVED) {
            if (!Objects.equals(attempt.getPgRef(), confirmedPaymentKey)) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "PG 결제 키가 기존 승인 결과와 일치하지 않습니다.");
            }
            return;
        }
        attempt.markApproved(confirmedPaymentKey, LocalDateTime.now(clock));
        attemptStore.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPgFailure(Long attemptId, String reason, boolean retryable) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (retryable) {
            attempt.markRetryable(reason);
        } else {
            attempt.markFailed(reason);
        }
        attemptStore.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmResult fulfillAndConfirm(Long attemptId) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "승인된 결제만 도메인 생성에 사용할 수 있습니다.");
        }
        PaymentPayload payload = deserialize(attempt.getPayloadJson());
        PaymentFulfiller fulfiller = fulfiller(attempt.getContext());
        PaymentFulfiller.FulfillResult fulfilled = fulfiller.fulfill(payload, attempt.getPgRef());
        attempt.markConfirmed();
        attemptStore.save(attempt);
        return new ConfirmResult(attempt.getContext(), fulfilled.domainId(), fulfilled.rawAccessToken());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requestCompensation(Long attemptId, String confirmedPaymentKey, String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() == PaymentAttemptStatus.PROCESSING) {
            attempt.markApproved(confirmedPaymentKey, LocalDateTime.now(clock));
        } else if (attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "승인된 결제만 보상 환불을 요청할 수 있습니다.");
        }
        if (!Objects.equals(attempt.getPgRef(), confirmedPaymentKey)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "PG 결제 키가 기존 승인 결과와 일치하지 않습니다.");
        }
        attempt.markCompensationRequested(reason);
        attemptStore.save(attempt);
        refundExecutionService.requestPaymentAttemptRefund(
                attempt.getId(), attempt.getAmount(), confirmedPaymentKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markZeroAmountFulfillmentFailed(Long attemptId, String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        attempt.markFailed(reason);
        attemptStore.save(attempt);
    }

    private PaymentAttempt findForUpdate(Long attemptId) {
        return attemptReader.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new NotFoundException("결제 시도"));
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

    private PaymentPayload deserialize(String json) {
        return objectMapper.readValue(json, PaymentPayload.class);
    }

    private boolean isStale(PaymentAttempt attempt, LocalDateTime now) {
        return attempt.getProcessingAt() == null
                || !attempt.getProcessingAt().isAfter(now.minus(PROCESSING_STALE_AFTER));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    record ClaimedAttempt(Long id, String orderId, long amount, String paymentKey,
                          String confirmedPaymentKey, boolean approved) {

        static ClaimedAttempt processing(PaymentAttempt attempt) {
            return new ClaimedAttempt(
                    attempt.getId(), attempt.getOrderIdExternal(), attempt.getAmount(),
                    attempt.getPaymentKey(), null, false);
        }

        static ClaimedAttempt approved(PaymentAttempt attempt) {
            return new ClaimedAttempt(
                    attempt.getId(), attempt.getOrderIdExternal(), attempt.getAmount(),
                    attempt.getPaymentKey(), attempt.getPgRef(), true);
        }

        String idempotencyKey() {
            return orderId;
        }
    }
}
