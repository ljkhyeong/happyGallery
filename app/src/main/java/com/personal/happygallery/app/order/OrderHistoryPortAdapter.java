package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link OrderApprovalHistoryRepository}(infra) → {@link OrderHistoryPort}(app) 브릿지 어댑터.
 */
@Component
class OrderHistoryPortAdapter implements OrderHistoryPort {

    private final OrderApprovalHistoryRepository orderApprovalHistoryRepository;

    OrderHistoryPortAdapter(OrderApprovalHistoryRepository orderApprovalHistoryRepository) {
        this.orderApprovalHistoryRepository = orderApprovalHistoryRepository;
    }

    @Override
    public OrderApprovalHistory save(OrderApprovalHistory history) {
        return orderApprovalHistoryRepository.save(history);
    }

    @Override
    public List<OrderApprovalHistory> findByOrderId(Long orderId) {
        return orderApprovalHistoryRepository.findByOrderId(orderId);
    }

    @Override
    public List<OrderApprovalHistory> findByOrderIdOrderByDecidedAtAsc(Long orderId) {
        return orderApprovalHistoryRepository.findByOrderIdOrderByDecidedAtAsc(orderId);
    }
}
