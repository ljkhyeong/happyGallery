package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.util.List;

public interface OrderHistoryPort {
    <S extends OrderApprovalHistory> S save(S history);
    List<OrderApprovalHistory> findByOrderIdOrderByDecidedAtAsc(Long orderId);
}
