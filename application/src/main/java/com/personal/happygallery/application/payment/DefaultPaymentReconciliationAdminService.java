package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.PaymentReconciliationTransactionService.LookupRequest;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

@Service
public class DefaultPaymentReconciliationAdminService implements PaymentReconciliationAdminUseCase {

    private static final int LIST_LIMIT = 100;

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentPort paymentPort;
    private final PaymentReconciliationTransactionService reconciliationTransactionService;
    private final PaymentConfirmTransactionService confirmTransactionService;

    public DefaultPaymentReconciliationAdminService(
            PaymentAttemptReaderPort attemptReader,
            PaymentPort paymentPort,
            PaymentReconciliationTransactionService reconciliationTransactionService,
            PaymentConfirmTransactionService confirmTransactionService) {
        this.attemptReader = attemptReader;
        this.paymentPort = paymentPort;
        this.reconciliationTransactionService = reconciliationTransactionService;
        this.confirmTransactionService = confirmTransactionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAttempt> listRequired() {
        return attemptReader.findReconciliationRequired(LIST_LIMIT);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public ReconciliationResult reconcile(Long attemptId) {
        LookupRequest request = reconciliationTransactionService.prepareLookup(attemptId);
        PaymentLookupResult lookup = requireNonNull(
                paymentPort.lookupByOrderId(request.orderId()),
                "PaymentPort.lookupByOrderId는 null을 반환할 수 없습니다.");
        return switch (lookup.status()) {
            case APPROVED -> completeApprovedPayment(request, lookup);
            case NOT_APPROVED -> completeWithoutApproval(request, lookup);
            case REVIEW_REQUIRED, UNAVAILABLE -> new ReconciliationResult(
                    request.attemptId(),
                    PaymentAttemptStatus.RECONCILIATION_REQUIRED,
                    null,
                    lookup.reason());
        };
    }

    private ReconciliationResult completeApprovedPayment(LookupRequest request,
                                                         PaymentLookupResult lookup) {
        String confirmedPaymentKey = reconciliationTransactionService.recordApproved(
                request.attemptId(), lookup);
        try {
            ConfirmResult result = confirmTransactionService.fulfillAndConfirm(request.attemptId());
            return new ReconciliationResult(
                    request.attemptId(),
                    PaymentAttemptStatus.CONFIRMED,
                    result.domainId(),
                    "PG 승인 확인 후 서비스 처리를 완료했습니다.");
        } catch (RuntimeException fulfillmentFailure) {
            try {
                confirmTransactionService.requestCompensationAfterFulfillmentFailure(
                        request.attemptId(),
                        confirmedPaymentKey,
                        "PG 대사 승인 후 도메인 생성에 실패했습니다.");
            } catch (RuntimeException compensationFailure) {
                fulfillmentFailure.addSuppressed(compensationFailure);
            }
            throw fulfillmentFailure;
        }
    }

    private ReconciliationResult completeWithoutApproval(LookupRequest request,
                                                         PaymentLookupResult lookup) {
        PaymentAttempt attempt = reconciliationTransactionService.recordNotApproved(
                request.attemptId(), lookup.reason());
        return new ReconciliationResult(
                attempt.getId(),
                attempt.getStatus(),
                null,
                lookup.reason());
    }
}
