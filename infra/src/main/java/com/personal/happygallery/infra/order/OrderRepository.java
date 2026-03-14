package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 자동환불 배치용 조회.
     * {@code status = PAID_APPROVAL_PENDING} AND {@code approvalDeadlineAt < deadline}.
     */
    List<Order> findByStatusAndApprovalDeadlineAtBefore(OrderStatus status, LocalDateTime deadline);

    /** 회원 — 자기 주문 조회 (최신순) */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 관리자 — 상태별 주문 조회 (최신순) */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** 관리자 — 전체 주문 조회 (최신순) */
    List<Order> findAllByOrderByCreatedAtDesc();
}
