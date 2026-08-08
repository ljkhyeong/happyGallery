package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface OrderReaderPort {
    Optional<Order> findById(Long id);
    Optional<Order> findByIdForUpdate(Long id);
    List<Order> findByIdIn(Collection<Long> ids);
    List<Order> findPaidApprovalPendingBeforeAfterId(
            LocalDateTime deadline, Long afterId, int limit);
    OrderApprovalBacklogSummary summarizePendingApprovalBacklog();
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId, int limit);
    List<Order> findByUserIdOrderByCreatedAtDescAfterCursor(
            Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit);
    /** 커서 기반 전체 주문 조회 — 첫 페이지 */
    List<Order> findAllOrderByCreatedAtDesc(int limit);

    /** 커서 기반 전체 주문 조회 — 커서 이후 */
    List<Order> findAllOrderByCreatedAtDescAfterCursor(
            LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    /** 커서 기반 상태별 주문 조회 — 첫 페이지 */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, int limit);

    /** 커서 기반 상태별 주문 조회 — 커서 이후 */
    List<Order> findByStatusOrderByCreatedAtDescAfterCursor(
            OrderStatus status, LocalDateTime cursorCreatedAt, Long cursorId, int limit);
}
