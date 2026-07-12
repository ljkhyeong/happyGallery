package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long>, RefundPort {

    @Override Optional<Refund> findById(Long id);
    @Override Refund save(Refund refund);

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findByStatusIn(List<RefundStatus> statuses);

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
