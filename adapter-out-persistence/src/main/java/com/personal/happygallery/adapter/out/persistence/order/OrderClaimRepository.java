package com.personal.happygallery.adapter.out.persistence.order;

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

public interface OrderClaimRepository extends JpaRepository<OrderClaim, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM OrderClaim c WHERE c.id = :id")
    Optional<OrderClaim> findByIdForUpdate(@Param("id") Long id);

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

    @Query("SELECT c FROM OrderClaim c ORDER BY c.requestedAt DESC, c.id DESC")
    List<OrderClaim> findRecentPage(Pageable pageable);

    @Query("""
            SELECT c FROM OrderClaim c
            WHERE c.requestedAt < :requestedAt
               OR (c.requestedAt = :requestedAt AND c.id < :id)
            ORDER BY c.requestedAt DESC, c.id DESC
            """)
    List<OrderClaim> findRecentAfterPage(
            @Param("requestedAt") LocalDateTime requestedAt,
            @Param("id") Long id,
            Pageable pageable);

    @Query("""
            SELECT c FROM OrderClaim c
            WHERE c.status = :status
            ORDER BY c.requestedAt DESC, c.id DESC
            """)
    List<OrderClaim> findRecentByStatusPage(
            @Param("status") OrderClaimStatus status, Pageable pageable);

    @Query("""
            SELECT c FROM OrderClaim c
            WHERE c.status = :status
              AND (c.requestedAt < :requestedAt
                   OR (c.requestedAt = :requestedAt AND c.id < :id))
            ORDER BY c.requestedAt DESC, c.id DESC
            """)
    List<OrderClaim> findRecentByStatusAfterPage(
            @Param("status") OrderClaimStatus status,
            @Param("requestedAt") LocalDateTime requestedAt,
            @Param("id") Long id,
            Pageable pageable);

    default List<OrderClaim> findRecent(int limit) {
        return findRecentPage(PageRequest.ofSize(limit));
    }

    default List<OrderClaim> findRecentAfter(LocalDateTime requestedAt, Long id, int limit) {
        return findRecentAfterPage(requestedAt, id, PageRequest.ofSize(limit));
    }

    default List<OrderClaim> findRecentByStatus(OrderClaimStatus status, int limit) {
        return findRecentByStatusPage(status, PageRequest.ofSize(limit));
    }

    default List<OrderClaim> findRecentByStatusAfter(
            OrderClaimStatus status, LocalDateTime requestedAt, Long id, int limit) {
        return findRecentByStatusAfterPage(
                status, requestedAt, id, PageRequest.ofSize(limit));
    }
}
