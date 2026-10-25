package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
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
import org.springframework.util.StringUtils;

@Service
class PaymentReconciliationTransactionService {

    private final PaymentAttemptReaderPort attemptReader;
    private final OrderPaymentBenefitReservationService benefitReservationService;
    private final Clock clock;

    PaymentReconciliationTransactionService(PaymentAttemptReaderPort attemptReader,
                                            OrderPaymentBenefitReservationService benefitReservationService,
                                            Clock clock) {
        this.attemptReader = attemptReader;
        this.benefitReservationService = benefitReservationService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String prepareLookup(Long attemptId) {
        return requireReconciliationAttempt(attemptId).getOrderIdExternal();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String recordApproved(Long attemptId, PaymentLookupResult lookup) {
        PaymentAttempt attempt = requireReconciliationAttempt(attemptId);
        if (!Objects.equals(attempt.getOrderIdExternal(), lookup.orderId())
                || attempt.getAmount() != lookup.totalAmount()
                || !StringUtils.hasText(lookup.paymentKey())
                || !Objects.equals(attempt.getPaymentKey(), lookup.paymentKey())) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "PG 조회 결과가 저장된 결제 요청과 일치하지 않습니다.");
        }
        // 잠금 아래 RECONCILIATION_REQUIRED임을 확인했으므로 늦은 승인 반영은 항상 APPROVED로 바뀐다.
        attempt.reconcileLatePgApproval(
                lookup.paymentKey(), lookup.method(), null, LocalDateTime.now(clock));
        return lookup.paymentKey();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentAttempt recordNotApproved(Long attemptId, String reason) {
        PaymentAttempt attempt = requireReconciliationAttempt(attemptId);
        benefitReservationService.releaseAfter(attempt, () -> {
            attempt.markReconciledNotApproved(reason);
            return true;
        }, LocalDateTime.now(clock));
        return attempt;
    }

    private PaymentAttempt requireReconciliationAttempt(Long attemptId) {
        PaymentAttempt attempt = attemptReader.findByIdForUpdate(attemptId)
                .orElseThrow(NotFoundException.supplier("결제 시도"));
        if (attempt.getStatus() != PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "대사가 필요한 결제만 처리할 수 있습니다.");
        }
        return attempt;
    }

}
