package com.personal.happygallery.app.order.port.out;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface OrderReaderPort {
    Optional<Order> findById(Long id);
    List<Order> findByStatusAndApprovalDeadlineAtBefore(OrderStatus status, LocalDateTime deadline);
    List<Order> findByStatusAndApprovalDeadlineAtBefore(OrderStatus status, LocalDateTime deadline, Pageable pageable);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();

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
