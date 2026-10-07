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
    /** 관리자 전체 주문 최신순 커서 조회. 커서 값이 null이면 첫 페이지를 읽는다. */
    List<Order> findAdminOrders(LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    /** 관리자 상태별 주문 최신순 커서 조회. 커서 값이 null이면 첫 페이지를 읽는다. */
    List<Order> findAdminOrdersByStatus(
            OrderStatus status, LocalDateTime cursorCreatedAt, Long cursorId, int limit);
}
