package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.util.List;

public interface OrderHistoryPort {
    OrderApprovalHistory save(OrderApprovalHistory history);
    List<OrderApprovalHistory> findByOrderId(Long orderId);
    List<OrderApprovalHistory> findByOrderIdOrderByDecidedAtAsc(Long orderId);
}
