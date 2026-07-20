package com.personal.happygallery.adapter.out.persistence.booking;

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

    @Override Optional<Refund> findById(Long id);
    @Override Refund save(Refund refund);
    @Override Optional<Refund> findByBookingId(Long bookingId);
    @Override Optional<Refund> findByOrderId(Long orderId);
    @Override Optional<Refund> findByPassPurchaseId(Long passPurchaseId);
    @Override List<Refund> findByPassPurchaseIdIn(List<Long> passPurchaseIds);

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
            ORDER BY created_at, id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findRecoverableIds(@Param("now") LocalDateTime now,
                                  @Param("staleBefore") LocalDateTime staleBefore,
                                  @Param("limit") int limit);
}
