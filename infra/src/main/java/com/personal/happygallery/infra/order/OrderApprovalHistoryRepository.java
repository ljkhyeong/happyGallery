package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderApprovalHistoryRepository extends JpaRepository<OrderApprovalHistory, Long> {

    List<OrderApprovalHistory> findByOrderId(Long orderId);

    List<OrderApprovalHistory> findByOrderIdOrderByDecidedAtAsc(Long orderId);
}
