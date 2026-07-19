package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    @Query(value = """
            SELECT id
            FROM payment_attempt
            WHERE (
                    (
                        status IN ('PROCESSING', 'RETRYABLE')
                        AND (
                            processing_at <= :staleBefore
                            OR (processing_at IS NULL AND created_at <= :staleBefore)
                        )
                    )
                    OR (
                        status = 'APPROVED'
                        AND (
                            confirmed_at <= :staleBefore
                            OR (confirmed_at IS NULL AND created_at <= :staleBefore)
                        )
                    )
                  )
              AND (
                    confirm_recovery_attempted_at IS NULL
                    OR confirm_recovery_attempted_at <= :staleBefore
                  )
            ORDER BY GREATEST(
                        CASE
                            WHEN status = 'APPROVED' THEN COALESCE(confirmed_at, created_at)
                            ELSE COALESCE(processing_at, created_at)
                        END,
                        COALESCE(confirm_recovery_attempted_at, created_at)
                     ),
                     id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findConfirmRecoveryCandidateIds(@Param("staleBefore") LocalDateTime staleBefore,
                                               @Param("limit") int limit);
}
