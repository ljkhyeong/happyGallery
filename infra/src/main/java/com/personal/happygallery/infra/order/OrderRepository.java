package com.personal.happygallery.infra.order;

import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderReaderPort, OrderStorePort {

    @Override Optional<Order> findById(Long id);
    @Override Order save(Order order);
    @Override Order saveAndFlush(Order order);

    /**
     * 자동환불 배치용 조회.
     * {@code status = PAID_APPROVAL_PENDING} AND {@code approvalDeadlineAt < deadline}.
     */
    List<Order> findByStatusAndApprovalDeadlineAtBefore(OrderStatus status, LocalDateTime deadline);
    List<Order> findByStatusAndApprovalDeadlineAtBefore(OrderStatus status, LocalDateTime deadline, Pageable pageable);

    /** 회원 — 자기 주문 조회 (최신순) */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** guest claim preview용 비회원 주문 조회 (최신순) */
    List<Order> findByGuestIdOrderByCreatedAtDesc(Long guestId);

    /** 관리자 — 상태별 주문 조회 (최신순) */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** 관리자 — 전체 주문 조회 (최신순) */
    List<Order> findAllByOrderByCreatedAtDesc();

    // ── 커서 기반 페이지네이션 ──

    /** 전체 주문 — 첫 페이지 */
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC, o.id DESC LIMIT :limit")
    List<Order> findAllOrderByCreatedAtDesc(@Param("limit") int limit);

    /** 전체 주문 — 커서 이후 */
    @Query("""
            SELECT o FROM Order o
            WHERE o.createdAt < :cursorCreatedAt
               OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId)
            ORDER BY o.createdAt DESC, o.id DESC
            LIMIT :limit
            """)
    List<Order> findAllOrderByCreatedAtDescAfterCursor(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);

    /** 상태별 주문 — 첫 페이지 */
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC, o.id DESC LIMIT :limit")
    List<Order> findByStatusOrderByCreatedAtDesc(
            @Param("status") OrderStatus status,
            @Param("limit") int limit);

    /** 상태별 주문 — 커서 이후 */
    @Query("""
            SELECT o FROM Order o
            WHERE o.status = :status
              AND (o.createdAt < :cursorCreatedAt
                   OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId))
            ORDER BY o.createdAt DESC, o.id DESC
            LIMIT :limit
            """)
    List<Order> findByStatusOrderByCreatedAtDescAfterCursor(
            @Param("status") OrderStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);
}
