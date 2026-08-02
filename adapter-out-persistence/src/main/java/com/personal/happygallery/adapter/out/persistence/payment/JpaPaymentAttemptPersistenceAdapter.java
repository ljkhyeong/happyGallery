package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import org.springframework.stereotype.Repository;

@Repository
class JpaPaymentAttemptPersistenceAdapter implements PaymentAttemptStorePort {

    private final PaymentAttemptRepository repository;

    JpaPaymentAttemptPersistenceAdapter(PaymentAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        return repository.save(attempt);
    }
}
