package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderStateProbe {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final RefundRepository refundRepository;
    private final OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    private final FulfillmentRepository fulfillmentRepository;

    public OrderStateProbe(OrderRepository orderRepository,
                           InventoryRepository inventoryRepository,
                           RefundRepository refundRepository,
                           OrderApprovalHistoryRepository orderApprovalHistoryRepository,
                           FulfillmentRepository fulfillmentRepository) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.refundRepository = refundRepository;
        this.orderApprovalHistoryRepository = orderApprovalHistoryRepository;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow();
    }

    public Inventory getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId).orElseThrow();
    }

    public List<Refund> refunds() {
        return refundRepository.findAll();
    }

    public long refundCount() {
        return refundRepository.count();
    }

    public List<OrderApprovalHistory> orderApprovalHistory(Long orderId) {
        return orderApprovalHistoryRepository.findByOrderId(orderId);
    }

    public List<OrderApprovalHistory> orderApprovalHistoryOrdered(Long orderId) {
        return orderApprovalHistoryRepository.findByOrderIdOrderByDecidedAtAsc(orderId);
    }

    public Optional<Fulfillment> findFulfillmentByOrderId(Long orderId) {
        return fulfillmentRepository.findByOrderId(orderId);
    }

    public List<Fulfillment> fulfillments() {
        return fulfillmentRepository.findAll();
    }

    public void deleteInventory(Long inventoryId) {
        inventoryRepository.deleteById(inventoryId);
    }
}
