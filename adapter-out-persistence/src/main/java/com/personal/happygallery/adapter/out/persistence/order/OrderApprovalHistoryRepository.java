package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderHistoryPort;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderApprovalHistoryRepository extends JpaRepository<OrderApprovalHistory, Long>,
        OrderHistoryPort {

    @Override
    <S extends OrderApprovalHistory> S save(S history);

    List<OrderApprovalHistory> findByOrderId(Long orderId);

    @Override
    List<OrderApprovalHistory> findByOrderIdOrderByDecidedAtAsc(Long orderId);
}
