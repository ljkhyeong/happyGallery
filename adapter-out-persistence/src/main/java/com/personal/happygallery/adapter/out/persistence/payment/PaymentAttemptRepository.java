package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentAttemptRepository
        extends JpaRepository<PaymentAttempt, Long>, PaymentAttemptReaderPort, PaymentAttemptStorePort {

    @Override Optional<PaymentAttempt> findById(Long id);
    @Override PaymentAttempt save(PaymentAttempt attempt);

    @Override Optional<PaymentAttempt> findByOrderIdExternal(String orderIdExternal);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from PaymentAttempt attempt where attempt.id = :id")
    Optional<PaymentAttempt> findByIdForUpdate(@Param("id") Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from PaymentAttempt attempt where attempt.orderIdExternal = :orderIdExternal")
    Optional<PaymentAttempt> findByOrderIdExternalForUpdate(@Param("orderIdExternal") String orderIdExternal);
}
