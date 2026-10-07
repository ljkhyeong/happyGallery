package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimStatus;
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

public interface OrderClaimRepository extends JpaRepository<OrderClaim, Long>, OrderClaimPort {

    @Override
    <S extends OrderClaim> S save(S claim);

    @Override
    @Query("SELECT c.orderId FROM OrderClaim c WHERE c.id = :id")
    Optional<Long> findOrderIdById(@Param("id") Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM OrderClaim c WHERE c.id = :id")
    Optional<OrderClaim> findByIdForUpdate(@Param("id") Long id);

    @Override
    List<OrderClaim> findByOrderIdOrderByRequestedAtDesc(Long orderId);

    @Query("""
            SELECT CASE WHEN COUNT(claim) > 0 THEN true ELSE false END
            FROM OrderClaim claim
            WHERE claim.status IN (
                com.personal.happygallery.domain.order.OrderClaimStatus.REQUESTED,
                com.personal.happygallery.domain.order.OrderClaimStatus.REFUND_REQUESTED,
                com.personal.happygallery.domain.order.OrderClaimStatus.EXCHANGE_APPROVED
            )
              AND claim.orderId IN (
                  SELECT o.id FROM Order o WHERE o.userId = :userId
              )
            """)
    boolean existsActiveByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM OrderClaim c
            WHERE :cursorRequestedAt IS NULL
               OR c.requestedAt < :cursorRequestedAt
               OR (c.requestedAt = :cursorRequestedAt AND c.id < :cursorId)
            ORDER BY c.requestedAt DESC, c.id DESC
            """)
    List<OrderClaim> findRecentPage(
            @Param("cursorRequestedAt") LocalDateTime cursorRequestedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("""
            SELECT c FROM OrderClaim c
            WHERE c.status = :status
              AND (:cursorRequestedAt IS NULL
                   OR c.requestedAt < :cursorRequestedAt
                   OR (c.requestedAt = :cursorRequestedAt AND c.id < :cursorId))
            ORDER BY c.requestedAt DESC, c.id DESC
            """)
    List<OrderClaim> findRecentPageByStatus(
            @Param("status") OrderClaimStatus status,
            @Param("cursorRequestedAt") LocalDateTime cursorRequestedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Override
    default List<OrderClaim> findRecent(LocalDateTime cursorRequestedAt, Long cursorId, int limit) {
        return findRecentPage(cursorRequestedAt, cursorId, PageRequest.ofSize(limit));
    }

    @Override
    default List<OrderClaim> findRecentByStatus(
            OrderClaimStatus status, LocalDateTime cursorRequestedAt, Long cursorId, int limit) {
        return findRecentPageByStatus(status, cursorRequestedAt, cursorId, PageRequest.ofSize(limit));
    }
}
