package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptBacklogSummary;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
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

    @Override
    <S extends PaymentAttempt> S save(S attempt);

    @Override Optional<PaymentAttempt> findById(Long id);

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
    @Query("""
            select (count(attempt) > 0)
            from PaymentAttempt attempt
            where attempt.ownerUserId = :userId
              and attempt.status not in (
                    com.personal.happygallery.domain.payment.PaymentAttemptStatus.CONFIRMED,
                    com.personal.happygallery.domain.payment.PaymentAttemptStatus.FAILED,
                    com.personal.happygallery.domain.payment.PaymentAttemptStatus.COMPENSATED,
                    com.personal.happygallery.domain.payment.PaymentAttemptStatus.CANCELED
                  )
            """)
    boolean existsNonTerminalByOwnerUserId(@Param("userId") Long userId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attempt
            from PaymentAttempt attempt
            where attempt.ownerUserId is null
              and attempt.ownerPhoneHmac in :phoneHmacCandidates
              and (attempt.status not in :terminalStatuses or attempt.createdAt >= :terminalCutoff)
            order by attempt.id
            """)
    List<PaymentAttempt> findGuestRecoveryCandidatesForUpdate(
            @Param("phoneHmacCandidates") List<String> phoneHmacCandidates,
            @Param("terminalStatuses") List<PaymentAttemptStatus> terminalStatuses,
            @Param("terminalCutoff") LocalDateTime terminalCutoff);

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
              AND id > :afterId
            ORDER BY id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findExpiredPendingIdsAfterId(
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("afterId") Long afterId,
            @Param("limit") int limit);

    @Override
    @Query(value = """
            SELECT id
            FROM payment_attempt
            WHERE status IN ('CONFIRMED', 'FAILED', 'COMPENSATED', 'CANCELED')
              AND created_at <= :createdBefore
              AND id > :afterId
              AND (payload_enc IS NOT NULL
                   OR fulfilled_access_token_enc IS NOT NULL
                   OR owner_phone_hmac IS NOT NULL
                   OR status_access_token_hash IS NOT NULL)
            ORDER BY id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findSensitiveDataCleanupCandidateIds(
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("afterId") Long afterId,
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

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.payment.port.out.PaymentAttemptBacklogSummary(
                       COUNT(attempt),
                       MIN(attempt.processingAt)
                   )
            FROM PaymentAttempt attempt
            WHERE attempt.status = com.personal.happygallery.domain.payment.PaymentAttemptStatus.RECONCILIATION_REQUIRED
            """)
    PaymentAttemptBacklogSummary summarizeReconciliationRequiredBacklog();
}
