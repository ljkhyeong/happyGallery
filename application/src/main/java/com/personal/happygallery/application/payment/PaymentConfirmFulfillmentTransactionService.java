package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentConfirmFulfillmentTransactionService {

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final RefundExecutionService refundExecutionService;
    private final PaymentConfirmAttemptResolver attemptResolver;
    private final OrderPaymentBenefitReservationService benefitReservationService;
    private final FieldEncryptor fieldEncryptor;
    private final Clock clock;

    PaymentConfirmFulfillmentTransactionService(PaymentAttemptReaderPort attemptReader,
                                                PaymentAttemptStorePort attemptStore,
                                                RefundExecutionService refundExecutionService,
                                                PaymentConfirmAttemptResolver attemptResolver,
                                                OrderPaymentBenefitReservationService benefitReservationService,
                                                FieldEncryptor fieldEncryptor,
                                                Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.refundExecutionService = refundExecutionService;
        this.attemptResolver = attemptResolver;
        this.benefitReservationService = benefitReservationService;
        this.fieldEncryptor = fieldEncryptor;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmResult fulfillAndConfirm(Long attemptId) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() == PaymentAttemptStatus.CONFIRMED) {
            return attemptResolver.confirmedResult(attempt);
        }
        if (attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                    "승인된 결제만 도메인 생성에 사용할 수 있습니다.");
        }
        PreparedPaymentPayload payload = attemptResolver.readPayload(attempt);
        PaymentFulfiller.FulfillResult fulfilled = attemptResolver.fulfill(attempt, payload);
        String accessTokenEnc = fulfilled.rawAccessToken() == null
                ? null
                : fieldEncryptor.encrypt(fulfilled.rawAccessToken());
        attempt.markConfirmed(fulfilled.domainId(), accessTokenEnc);
        attemptStore.save(attempt);
        return attemptResolver.confirmedResult(attempt);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkZeroAmountFulfillmentFailed(Long attemptId, String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        if (attempt.getStatus() != PaymentAttemptStatus.APPROVED) {
            return false;
        }
        PreparedPaymentPayload payload = benefitReservationService.readPayloadForRelease(attempt);
        attempt.markFailed(reason);
        attemptStore.save(attempt);
        if (payload != null) {
            benefitReservationService.release(
                    attempt, payload, LocalDateTime.now(clock));
        }
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

    private PaymentAttempt findForUpdate(Long attemptId) {
        return attemptReader.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new NotFoundException("결제 시도"));
    }

    private void requireSameConfirmedPaymentKey(PaymentAttempt attempt, String confirmedPaymentKey) {
        if (!Objects.equals(attempt.getConfirmedPaymentKey(), confirmedPaymentKey)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "PG 결제 키가 기존 승인 결과와 일치하지 않습니다.");
        }
    }
}
