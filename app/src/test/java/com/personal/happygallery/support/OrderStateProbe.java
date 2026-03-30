package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderHistoryPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.payment.port.out.RefundPort;
import com.personal.happygallery.app.product.port.out.InventoryReaderPort;
import com.personal.happygallery.app.product.port.out.InventoryStorePort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderStateProbe {

    private final OrderReaderPort orderReaderPort;
    private final InventoryReaderPort inventoryReaderPort;
    private final InventoryStorePort inventoryStorePort;
    private final RefundPort refundPort;
    private final OrderHistoryPort orderHistoryPort;
    private final FulfillmentPort fulfillmentPort;

    public OrderStateProbe(OrderReaderPort orderReaderPort,
                           InventoryReaderPort inventoryReaderPort,
                           InventoryStorePort inventoryStorePort,
                           RefundPort refundPort,
                           OrderHistoryPort orderHistoryPort,
                           FulfillmentPort fulfillmentPort) {
        this.orderReaderPort = orderReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.inventoryStorePort = inventoryStorePort;
        this.refundPort = refundPort;
        this.orderHistoryPort = orderHistoryPort;
        this.fulfillmentPort = fulfillmentPort;
    }

    public Order getOrder(Long orderId) {
        return orderReaderPort.findById(orderId).orElseThrow();
    }

    public Inventory getInventoryByProductId(Long productId) {
        return inventoryReaderPort.findByProductId(productId).orElseThrow();
    }

    public List<Refund> refunds() {
        return refundPort.findAll();
    }

    public long refundCount() {
        return refundPort.count();
    }

    public List<OrderApprovalHistory> orderApprovalHistory(Long orderId) {
        return orderHistoryPort.findByOrderId(orderId);
    }

    public List<OrderApprovalHistory> orderApprovalHistoryOrdered(Long orderId) {
        return orderHistoryPort.findByOrderIdOrderByDecidedAtAsc(orderId);
    }

    public Optional<Fulfillment> findFulfillmentByOrderId(Long orderId) {
        return fulfillmentPort.findByOrderId(orderId);
    }

    public List<Fulfillment> fulfillments() {
        return fulfillmentPort.findAll();
    }

    public void deleteInventory(Long inventoryId) {
        inventoryStorePort.deleteById(inventoryId);
    }
}
