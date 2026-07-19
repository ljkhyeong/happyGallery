package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
                            processing_at <= :activityStaleBefore
                            OR (processing_at IS NULL AND created_at <= :createdAtStaleBeforeUtc)
                        )
                    )
                    OR (
                        status = 'APPROVED'
                        AND (
                            confirmed_at <= :activityStaleBefore
                            OR (confirmed_at IS NULL AND created_at <= :createdAtStaleBeforeUtc)
                        )
                    )
                  )
              AND (
                    confirm_recovery_attempted_at IS NULL
                    OR confirm_recovery_attempted_at <= :activityStaleBefore
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
    List<Long> findConfirmRecoveryCandidateIds(
                                               @Param("activityStaleBefore") LocalDateTime activityStaleBefore,
                                               @Param("createdAtStaleBeforeUtc") LocalDateTime createdAtStaleBeforeUtc,
                                               @Param("limit") int limit);

    @Override
    @Query(value = """
            SELECT id
            FROM payment_attempt
            WHERE status = 'PENDING'
              AND created_at <= :createdBefore
            ORDER BY created_at, id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findExpiredPendingIds(@Param("createdBefore") LocalDateTime createdBefore,
                                     @Param("limit") int limit);

    @Query("""
            SELECT attempt
            FROM PaymentAttempt attempt
            WHERE attempt.status = com.personal.happygallery.domain.payment.PaymentAttemptStatus.RECONCILIATION_REQUIRED
            ORDER BY attempt.createdAt, attempt.id
            """)
    List<PaymentAttempt> findReconciliationRequiredPage(Pageable pageable);

    @Override
    default List<PaymentAttempt> findReconciliationRequired(int limit) {
        return findReconciliationRequiredPage(PageRequest.ofSize(limit));
    }
}
