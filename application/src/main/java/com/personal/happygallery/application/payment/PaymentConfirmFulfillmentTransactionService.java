package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentConfirmFulfillmentTransactionService {

    private final PaymentAttemptReaderPort attemptReader;
    private final RefundExecutionService refundExecutionService;
    private final PaymentConfirmAttemptResolver attemptResolver;
    private final OrderPaymentBenefitReservationService benefitReservationService;
    private final FieldEncryptor fieldEncryptor;
    private final Clock clock;

    PaymentConfirmFulfillmentTransactionService(PaymentAttemptReaderPort attemptReader,
                                                RefundExecutionService refundExecutionService,
                                                PaymentConfirmAttemptResolver attemptResolver,
                                                OrderPaymentBenefitReservationService benefitReservationService,
                                                FieldEncryptor fieldEncryptor,
                                                Clock clock) {
        this.attemptReader = attemptReader;
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
        return attemptResolver.confirmedResult(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean requestCompensationForUnpersistedApproval(Long attemptId,
                                                             String processingToken,
                                                             String confirmedPaymentKey,
                                                             String confirmedPaymentMethod,
                                                             String reason) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        // markApproved는 PROCESSING 실행권 소유자만 승인으로 바꾸고, 그 밖의 상태는 변경 없이 false를 반환한다.
        if (!attempt.markApproved(
                processingToken, confirmedPaymentKey,
                confirmedPaymentMethod, null, LocalDateTime.now(clock))) {
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
        // markFailed는 payload를 지우지 않으므로 전이 뒤에 읽어도 같은 payload로 해제한다.
        attempt.markFailed(reason);
        benefitReservationService.release(attempt, LocalDateTime.now(clock));
        return true;
    }

    private void requestCompensation(PaymentAttempt attempt,
                                     String confirmedPaymentKey,
                                     String reason) {
        attempt.requireMatchingConfirmedPaymentKey(confirmedPaymentKey);
        attempt.markCompensationRequested(reason);
        refundExecutionService.requestPaymentAttemptRefund(
                attempt.getId(), attempt.getAmount(), confirmedPaymentKey);
    }

    private PaymentAttempt findForUpdate(Long attemptId) {
        return attemptReader.findByIdForUpdate(attemptId)
                .orElseThrow(NotFoundException.supplier("결제 시도"));
    }
}
