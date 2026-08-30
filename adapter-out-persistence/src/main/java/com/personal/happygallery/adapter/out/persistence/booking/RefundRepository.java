package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.payment.port.out.RefundBacklogSummary;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long>, RefundPort {

    @Override
    <S extends Refund> S save(S refund);

    @Override
    Optional<Refund> findById(Long id);

    @Override
    @Query(value = """
            SELECT * FROM refunds
            WHERE booking_id = :bookingId
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Refund> findLatestByBookingId(@Param("bookingId") Long bookingId);
    @Override
    @Query("SELECT r FROM Refund r WHERE r.orderId = :orderId AND r.orderClaimId IS NULL")
    Optional<Refund> findDirectByOrderId(@Param("orderId") Long orderId);
    @Override
    Optional<Refund> findByOrderClaimId(Long orderClaimId);
    @Override
    Optional<Refund> findByPassPurchaseId(Long passPurchaseId);
    @Override
    Optional<Refund> findByPaymentAttemptId(Long paymentAttemptId);
    @Override
    Optional<Refund> findByRefundTransactionKey(String refundTransactionKey);
    @Override
    List<Refund> findByPaymentAttemptIdIn(List<Long> paymentAttemptIds);
    @Override
    List<Refund> findByPassPurchaseIdIn(List<Long> passPurchaseIds);
    @Override
    List<Refund> findByOrderClaimIdIn(List<Long> orderClaimIds);

    @Override
    @Query("""
            SELECT COALESCE(SUM(r.rewardRevokeAmount), 0)
            FROM Refund r
            WHERE r.orderId = :orderId
            """)
    long sumRewardRevokeAmountByOrderId(@Param("orderId") Long orderId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Refund r
            WHERE r.status <> com.personal.happygallery.domain.payment.RefundStatus.SUCCEEDED
              AND (
                  r.orderId IN (SELECT o.id FROM Order o WHERE o.userId = :userId)
                  OR r.bookingId IN (SELECT b.id FROM Booking b WHERE b.userId = :userId)
                  OR r.passPurchaseId IN (SELECT p.id FROM PassPurchase p WHERE p.userId = :userId)
              )
            """)
    boolean existsUnresolvedByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT r FROM Refund r
            WHERE r.status IN (
                com.personal.happygallery.domain.payment.RefundStatus.FAILED,
                com.personal.happygallery.domain.payment.RefundStatus.RETRYABLE,
                com.personal.happygallery.domain.payment.RefundStatus.RECONCILIATION_REQUIRED
            )
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Refund> findActionRequiredPage(Pageable pageable);

    @Query("""
            SELECT r FROM Refund r
            WHERE r.status IN (
                com.personal.happygallery.domain.payment.RefundStatus.FAILED,
                com.personal.happygallery.domain.payment.RefundStatus.RETRYABLE,
                com.personal.happygallery.domain.payment.RefundStatus.RECONCILIATION_REQUIRED
            )
              AND (r.createdAt < :createdAt
                   OR (r.createdAt = :createdAt AND r.id < :id))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Refund> findActionRequiredAfterPage(@Param("createdAt") LocalDateTime createdAt,
                                             @Param("id") Long id,
                                             Pageable pageable);

    @Override
    default List<Refund> findActionRequired(int limit) {
        return findActionRequiredPage(PageRequest.ofSize(limit));
    }

    @Override
    default List<Refund> findActionRequiredAfter(LocalDateTime createdAt, Long id, int limit) {
        return findActionRequiredAfterPage(createdAt, id, PageRequest.ofSize(limit));
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Refund r WHERE r.id = :id")
    Optional<Refund> findByIdForUpdate(@Param("id") Long id);

    @Override
    @Query(value = """
            SELECT id
            FROM refunds
            WHERE (
                    status IN ('REQUESTED', 'RETRYABLE', 'RECONCILIATION_REQUIRED')
                    AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
                  )
               OR (status = 'PROCESSING' AND processing_at < :staleBefore)
            ORDER BY last_recovery_at, created_at, id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findRecoverableIds(@Param("now") LocalDateTime now,
                                  @Param("staleBefore") LocalDateTime staleBefore,
                                  @Param("limit") int limit);

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.payment.port.out.RefundBacklogSummary(
                r.status,
                COUNT(r),
                MIN(CASE
                    WHEN r.status = com.personal.happygallery.domain.payment.RefundStatus.REQUESTED
                        THEN r.createdAt
                    WHEN r.status IN (
                        com.personal.happygallery.domain.payment.RefundStatus.RETRYABLE,
                        com.personal.happygallery.domain.payment.RefundStatus.RECONCILIATION_REQUIRED
                    )
                        THEN COALESCE(r.nextAttemptAt, r.createdAt)
                    WHEN r.status = com.personal.happygallery.domain.payment.RefundStatus.PROCESSING
                        THEN COALESCE(r.processingAt, r.createdAt)
                    ELSE r.updatedAt
                END)
            )
            FROM Refund r
            WHERE r.status IN (
                com.personal.happygallery.domain.payment.RefundStatus.REQUESTED,
                com.personal.happygallery.domain.payment.RefundStatus.PROCESSING,
                com.personal.happygallery.domain.payment.RefundStatus.RETRYABLE,
                com.personal.happygallery.domain.payment.RefundStatus.RECONCILIATION_REQUIRED,
                com.personal.happygallery.domain.payment.RefundStatus.FAILED
            )
            GROUP BY r.status
            """)
    List<RefundBacklogSummary> summarizeUnresolvedBacklog();
}
