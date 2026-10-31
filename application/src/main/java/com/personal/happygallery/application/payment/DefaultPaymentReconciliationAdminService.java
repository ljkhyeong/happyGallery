package com.personal.happygallery.application.payment;

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
    private final PaymentFulfillmentCoordinator fulfillmentCoordinator;

    public DefaultPaymentReconciliationAdminService(
            PaymentAttemptReaderPort attemptReader,
            PaymentPort paymentPort,
            PaymentReconciliationTransactionService reconciliationTransactionService,
            PaymentFulfillmentCoordinator fulfillmentCoordinator) {
        this.attemptReader = attemptReader;
        this.paymentPort = paymentPort;
        this.reconciliationTransactionService = reconciliationTransactionService;
        this.fulfillmentCoordinator = fulfillmentCoordinator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAttempt> listRequired() {
        return attemptReader.findReconciliationRequired(LIST_LIMIT);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public ReconciliationResult reconcile(Long attemptId) {
        String orderId = reconciliationTransactionService.prepareLookup(attemptId);
        PaymentLookupResult lookup = requireNonNull(
                paymentPort.lookupByOrderId(orderId),
                "PaymentPort.lookupByOrderId는 null을 반환할 수 없습니다.");
        return switch (lookup.status()) {
            case APPROVED -> completeApprovedPayment(attemptId, orderId, lookup);
            case NOT_APPROVED -> completeWithoutApproval(attemptId, lookup);
            case REVIEW_REQUIRED, UNAVAILABLE -> new ReconciliationResult(
                    attemptId,
                    PaymentAttemptStatus.RECONCILIATION_REQUIRED,
                    null,
                    lookup.reason(),
                    lookup.status() == PaymentLookupResult.Status.UNAVAILABLE);
        };
    }

    private ReconciliationResult completeApprovedPayment(Long attemptId,
                                                         String orderId,
                                                         PaymentLookupResult lookup) {
        String confirmedPaymentKey = reconciliationTransactionService.recordApproved(
                attemptId, lookup);
        // recordApproved가 저장 금액과 대조한 PG 승인 금액이며, 대사 대상은 PG를 호출한 유료 결제뿐이라 항상 보상 환불 경로를 탄다.
        ConfirmResult result = fulfillmentCoordinator.fulfillOrCompensate(
                attemptId, orderId, lookup.totalAmount(), confirmedPaymentKey,
                "PG 대사 승인 후 도메인 생성에 실패했습니다.");
        return new ReconciliationResult(
                attemptId,
                PaymentAttemptStatus.CONFIRMED,
                result.domainId(),
                "PG 승인 확인 후 서비스 처리를 완료했습니다.",
                false);
    }

    private ReconciliationResult completeWithoutApproval(Long attemptId,
                                                         PaymentLookupResult lookup) {
        PaymentAttempt attempt = reconciliationTransactionService.recordNotApproved(
                attemptId, lookup.reason());
        return new ReconciliationResult(
                attempt.getId(),
                attempt.getStatus(),
                null,
                lookup.reason(),
                false);
    }
}
