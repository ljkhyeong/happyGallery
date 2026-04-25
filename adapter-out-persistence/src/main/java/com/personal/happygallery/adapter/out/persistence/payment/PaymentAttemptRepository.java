package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository
        extends JpaRepository<PaymentAttempt, Long>, PaymentAttemptReaderPort, PaymentAttemptStorePort {

    @Override Optional<PaymentAttempt> findById(Long id);
    @Override PaymentAttempt save(PaymentAttempt attempt);
    @Override PaymentAttempt saveAndFlush(PaymentAttempt attempt);

    @Override Optional<PaymentAttempt> findByOrderIdExternal(String orderIdExternal);
}
