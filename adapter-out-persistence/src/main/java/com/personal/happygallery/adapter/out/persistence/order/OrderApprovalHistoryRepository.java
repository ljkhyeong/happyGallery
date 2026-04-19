package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderApprovalHistoryRepository extends JpaRepository<OrderApprovalHistory, Long>, OrderHistoryPort {

    @Override OrderApprovalHistory save(OrderApprovalHistory history);

    List<OrderApprovalHistory> findByOrderId(Long orderId);

    List<OrderApprovalHistory> findByOrderIdOrderByDecidedAtAsc(Long orderId);
}
