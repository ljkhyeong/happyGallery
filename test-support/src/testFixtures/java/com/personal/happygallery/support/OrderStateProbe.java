package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.order.FulfillmentRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderStateProbe {

    private final OrderReaderPort orderReaderPort;
    private final InventoryReaderPort inventoryReaderPort;
    private final RefundRepository refundRepository;
    private final OrderApprovalHistoryRepository orderHistoryRepository;
    private final FulfillmentRepository fulfillmentRepository;

    public OrderStateProbe(OrderReaderPort orderReaderPort,
                           InventoryReaderPort inventoryReaderPort,
                           RefundRepository refundRepository,
                           OrderApprovalHistoryRepository orderHistoryRepository,
                           FulfillmentRepository fulfillmentRepository) {
        this.orderReaderPort = orderReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.refundRepository = refundRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    public Order getOrder(Long orderId) {
        return orderReaderPort.findById(orderId).orElseThrow();
    }

    public Inventory getInventoryByProductId(Long productId) {
        return inventoryReaderPort.findByProductId(productId).orElseThrow();
    }

    public List<Refund> refunds() {
        return refundRepository.findAll();
    }

    public long refundCount() {
        return refundRepository.count();
    }

    public List<OrderApprovalHistory> orderApprovalHistory(Long orderId) {
        return orderHistoryRepository.findByOrderId(orderId);
    }

    public List<OrderApprovalHistory> orderApprovalHistoryOrdered(Long orderId) {
        return orderHistoryRepository.findByOrderIdOrderByDecidedAtAsc(orderId);
    }

    public Optional<Fulfillment> findFulfillmentByOrderId(Long orderId) {
        return fulfillmentRepository.findByOrderId(orderId);
    }
}
