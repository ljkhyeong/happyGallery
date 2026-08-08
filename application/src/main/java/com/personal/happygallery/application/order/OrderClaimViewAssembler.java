package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.application.order.port.out.OrderClaimItemPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimItem;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class OrderClaimViewAssembler {

    private final OrderReaderPort orderReader;
    private final OrderItemPort orderItemPort;
    private final OrderClaimItemPort orderClaimItemPort;
    private final RefundPort refundPort;

    OrderClaimViewAssembler(OrderReaderPort orderReader,
                            OrderItemPort orderItemPort,
                            OrderClaimItemPort orderClaimItemPort,
                            RefundPort refundPort) {
        this.orderReader = orderReader;
        this.orderItemPort = orderItemPort;
        this.orderClaimItemPort = orderClaimItemPort;
        this.refundPort = refundPort;
    }

    List<OrderClaimView> assemble(List<OrderClaim> claims) {
        if (claims.isEmpty()) {
            return List.of();
        }
        List<Long> claimIds = claims.stream().map(OrderClaim::getId).toList();
        List<OrderClaimItem> claimItems = orderClaimItemPort.findByClaimIdIn(claimIds);
        List<Long> orderIds = claims.stream().map(OrderClaim::getOrderId).distinct().toList();
        List<OrderItem> allOrderItems = orderItemPort.findByOrderIdIn(orderIds);
        Map<Long, OrderItem> orderItemsById = allOrderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<Long, List<OrderItem>> orderItemsByOrderId = allOrderItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        Map<Long, List<OrderClaimItem>> claimItemsByClaimId = claimItems.stream()
                .collect(Collectors.groupingBy(OrderClaimItem::getClaimId));
        Map<Long, Order> ordersById = orderReader.findByIdIn(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<Long, Refund> refundsByClaimId = refundPort.findByOrderClaimIdIn(claimIds).stream()
                .collect(Collectors.toMap(Refund::getOrderClaimId, Function.identity()));

        return claims.stream()
                .map(claim -> toView(
                        claim,
                        ordersById.get(claim.getOrderId()),
                        claimItemsByClaimId.getOrDefault(claim.getId(), List.of()),
                        orderItemsById,
                        orderItemsByOrderId.getOrDefault(claim.getOrderId(), List.of()),
                        refundsByClaimId.get(claim.getId())))
                .toList();
    }

    OrderClaimView assemble(OrderClaim claim) {
        return assemble(List.of(claim)).getFirst();
    }

    private OrderClaimView toView(OrderClaim claim,
                                  Order order,
                                  List<OrderClaimItem> claimItems,
                                  Map<Long, OrderItem> orderItemsById,
                                  List<OrderItem> allOrderItems,
                                  Refund refund) {
        List<OrderClaimLine> lines = claimItems.stream()
                .map(item -> new OrderClaimLine(
                        item, requireOrderItem(orderItemsById, item.getOrderItemId())))
                .toList();
        return new OrderClaimView(
                claim.getId(),
                claim.getOrderId(),
                claim.getType(),
                claim.getRequestedResolution(),
                claim.getStatus(),
                claim.getCustomerReason(),
                claim.getAdminNote(),
                claim.getResolvedByAdminId(),
                claim.getCompletedByAdminId(),
                claim.getReplacementCarrier(),
                claim.getReplacementTrackingNumber(),
                OrderClaimRefundCalculator.maximumRefundAmount(order, lines, allOrderItems),
                refund == null ? null : refund.getCustomerRefundAmount(),
                refund == null ? null : refund.getStatus(),
                claim.getRequestedAt(),
                claim.getResolvedAt(),
                claim.getCompletedAt(),
                lines.stream()
                        .map(line -> new OrderClaimView.Item(
                                line.orderItem().getId(),
                                line.orderItem().getProductId(),
                                line.orderItem().getProductName(),
                                line.claimItem().getQuantity(),
                                line.orderItem().getUnitPrice()))
                        .toList());
    }

    private OrderItem requireOrderItem(Map<Long, OrderItem> itemsById, Long orderItemId) {
        OrderItem item = itemsById.get(orderItemId);
        if (item == null) {
            throw new NotFoundException("주문 상품");
        }
        return item;
    }
}
