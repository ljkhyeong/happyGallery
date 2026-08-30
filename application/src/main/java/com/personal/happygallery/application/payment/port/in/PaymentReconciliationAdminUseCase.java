package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.util.List;

public interface PaymentReconciliationAdminUseCase {

    List<PaymentAttempt> listRequired();

    ReconciliationResult reconcile(Long attemptId);

    record ReconciliationResult(
            Long attemptId,
            PaymentAttemptStatus status,
            Long domainId,
            String message
    ) {}
}
