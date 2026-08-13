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

    /** 회원 — 자기 주문 조회 (최신순) */
    List<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.userId = :userId
              AND (o.createdAt < :cursorCreatedAt
                   OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId))
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    List<Order> findByUserIdOrderByCreatedAtDescAfterCursorPage(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Override
    default List<Order> findByUserIdOrderByCreatedAtDesc(Long userId, int limit) {
        return findByUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.ofSize(limit));
    }

    @Override
    default List<Order> findByUserIdOrderByCreatedAtDescAfterCursor(
            Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findByUserIdOrderByCreatedAtDescAfterCursorPage(
                userId, cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }

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

    List<Order> findByAccessTokenOrderByCreatedAtDescIdDesc(
            String accessToken, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.accessToken = :accessToken
              AND (o.createdAt < :createdAt
                   OR (o.createdAt = :createdAt AND o.id < :id))
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    List<Order> findByAccessTokenAfterPage(
            @Param("accessToken") String accessToken,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    // ── 커서 기반 페이지네이션 ──

    /** 전체 주문 — 첫 페이지 */
    @Override
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC, o.id DESC LIMIT :limit")
    List<Order> findAllOrderByCreatedAtDesc(@Param("limit") int limit);

    /** 전체 주문 — 커서 이후 (tuple comparison으로 복합 인덱스 range scan 활용) */
    @Override
    @Query(value = """
            SELECT id, user_id, guest_id, access_token, payment_key, status,
                   total_amount, product_amount, shipping_fee,
                   coupon_discount_amount, reward_used_amount, pg_paid_amount,
                   reward_earn_base, issued_coupon_id, paid_at, approval_deadline_at,
                   made_to_order_consent_version, made_to_order_consent_disclosure,
                   made_to_order_consent_at, version, created_at
            FROM orders
            WHERE (created_at, id) < (:cursorCreatedAt, :cursorId)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Order> findAllOrderByCreatedAtDescAfterCursor(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);

    /** 상태별 주문 — 첫 페이지 */
    @Override
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC, o.id DESC LIMIT :limit")
    List<Order> findByStatusOrderByCreatedAtDesc(
            @Param("status") OrderStatus status,
            @Param("limit") int limit);

    /** 상태별 주문 — 커서 이후 (tuple comparison으로 복합 인덱스 range scan 활용) */
    @Override
    @Query(value = """
            SELECT id, user_id, guest_id, access_token, payment_key, status,
                   total_amount, product_amount, shipping_fee,
                   coupon_discount_amount, reward_used_amount, pg_paid_amount,
                   reward_earn_base, issued_coupon_id, paid_at, approval_deadline_at,
                   made_to_order_consent_version, made_to_order_consent_disclosure,
                   made_to_order_consent_at, version, created_at
            FROM orders
            WHERE status = :#{#status.name()}
              AND (created_at, id) < (:cursorCreatedAt, :cursorId)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Order> findByStatusOrderByCreatedAtDescAfterCursor(
            @Param("status") OrderStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);
}
