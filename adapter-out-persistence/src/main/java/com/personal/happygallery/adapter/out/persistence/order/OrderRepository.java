package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderApprovalBacklogSummary;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderReaderPort, OrderStorePort {

    @Override
    <S extends Order> S save(S order);

    @Override
    <S extends Order> S saveAndFlush(S order);

    @Override Optional<Order> findById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Override List<Order> findByIdIn(Collection<Long> ids);

    /**
     * 자동환불 배치용 조회.
     * {@code status = PAID_APPROVAL_PENDING} AND {@code approvalDeadlineAt < deadline}.
     */
    @Query("""
            SELECT o FROM Order o
            WHERE o.status = com.personal.happygallery.domain.order.OrderStatus.PAID_APPROVAL_PENDING
              AND o.approvalDeadlineAt < :deadline
              AND o.id > :afterId
            ORDER BY o.id ASC
            """)
    List<Order> findPaidApprovalPendingBeforeAfterIdPage(
            @Param("deadline") LocalDateTime deadline,
            @Param("afterId") Long afterId,
            Pageable pageable);

    @Override
    default List<Order> findPaidApprovalPendingBeforeAfterId(
            LocalDateTime deadline, Long afterId, int limit) {
        return findPaidApprovalPendingBeforeAfterIdPage(
                deadline, afterId, PageRequest.ofSize(limit));
    }

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.order.port.out.OrderApprovalBacklogSummary(
                COUNT(o), MIN(o.paidAt))
            FROM Order o
            WHERE o.status = com.personal.happygallery.domain.order.OrderStatus.PAID_APPROVAL_PENDING
            """)
    OrderApprovalBacklogSummary summarizePendingApprovalBacklog();

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
            FROM Order o
            WHERE o.userId = :userId
              AND o.status IN (
                  com.personal.happygallery.domain.order.OrderStatus.PAID_APPROVAL_PENDING,
                  com.personal.happygallery.domain.order.OrderStatus.APPROVED_FULFILLMENT_PENDING,
                  com.personal.happygallery.domain.order.OrderStatus.IN_PRODUCTION,
                  com.personal.happygallery.domain.order.OrderStatus.DELAY_CONSENT_PENDING,
                  com.personal.happygallery.domain.order.OrderStatus.DELAY_ACCEPTED,
                  com.personal.happygallery.domain.order.OrderStatus.SHIPPING_PREPARING,
                  com.personal.happygallery.domain.order.OrderStatus.SHIPPED,
                  com.personal.happygallery.domain.order.OrderStatus.PICKUP_READY
              )
            """)
    boolean existsUnfinishedByUserId(@Param("userId") Long userId);

    /** guest claim preview용 비회원 주문 조회 (최신순) */
    List<Order> findByGuestIdOrderByCreatedAtDescIdDesc(Long guestId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.accessToken = :accessToken,
                o.version = o.version + 1
            WHERE o.guestId = :guestId
            """)
    int replaceAccessTokenByGuestId(
            @Param("guestId") Long guestId,
            @Param("accessToken") String accessToken);

    @Query("""
            SELECT o FROM Order o
            WHERE o.accessToken = :accessToken
              AND (:cursorCreatedAt IS NULL
                   OR o.createdAt < :cursorCreatedAt
                   OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId))
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    List<Order> findPageByAccessToken(
            @Param("accessToken") String accessToken,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    // ── 관리자 커서 기반 페이지네이션 ──

    /** 전체 주문 최신순 페이지. OR 확장 비교로 (created_at, id) 인덱스 범위 조회를 사용한다. */
    @Query("""
            SELECT o FROM Order o
            WHERE (:cursorCreatedAt IS NULL
                   OR o.createdAt < :cursorCreatedAt
                   OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId))
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    List<Order> findAdminOrderPage(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Override
    default List<Order> findAdminOrders(LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findAdminOrderPage(cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }

    /** 상태별 주문 최신순 페이지. (status, created_at, id) 인덱스 범위 조회를 사용한다. */
    @Query("""
            SELECT o FROM Order o
            WHERE o.status = :status
              AND (:cursorCreatedAt IS NULL
                   OR o.createdAt < :cursorCreatedAt
                   OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId))
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    List<Order> findAdminOrderPageByStatus(
            @Param("status") OrderStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Override
    default List<Order> findAdminOrdersByStatus(
            OrderStatus status, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findAdminOrderPageByStatus(status, cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }
}
