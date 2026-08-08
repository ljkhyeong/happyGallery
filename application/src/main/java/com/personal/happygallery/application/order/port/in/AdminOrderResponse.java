package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record AdminOrderResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalAmount,
        long productAmount,
        long shippingFee,
        long couponDiscountAmount,
        long rewardUsedAmount,
        long pgPaidAmount,
        long rewardEarnBase,
        Long issuedCouponId,
        FulfillmentType fulfillmentType,
        List<OrderItemView> items,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        OffsetDateTime createdAt
) {

    public AdminOrderResponse {
        items = List.copyOf(items);
    }

    public record OrderItemView(
            Long productId,
            String productName,
            ProductType productType,
            int qty,
            long unitPrice,
            long grossAmount,
            long couponDiscountAmount,
            long rewardUsedAmount,
            long netPaidAmount,
            String specification,
            String careInstructions,
            Integer productionLeadDays
    ) {
        public OrderItemView(
                Long productId,
                String productName,
                ProductType productType,
                int qty,
                long unitPrice,
                String specification,
                String careInstructions,
                Integer productionLeadDays) {
            this(productId, productName, productType, qty, unitPrice,
                    Math.multiplyExact(qty, unitPrice), 0L, 0L,
                    Math.multiplyExact(qty, unitPrice),
                    specification, careInstructions, productionLeadDays);
        }

        private static OrderItemView from(OrderItem item) {
            return new OrderItemView(
                    item.getProductId(),
                    item.getProductName(),
                    item.getProductType(),
                    item.getQty(),
                    item.getUnitPrice(),
                    item.getGrossAmount(),
                    item.getCouponDiscountAmount(),
                    item.getRewardUsedAmount(),
                    item.getNetPaidAmount(),
                    item.getSpecification(),
                    item.getCareInstructions(),
                    item.getProductionLeadDays());
        }
    }

    /** 기존 관리자 테스트·내부 호출의 전액 PG 주문 생성 편의 생성자. */
    public AdminOrderResponse(
            Long orderId,
            String orderNumber,
            OrderStatus status,
            long totalAmount,
            long shippingFee,
            FulfillmentType fulfillmentType,
            List<OrderItemView> items,
            LocalDateTime paidAt,
            LocalDateTime approvalDeadlineAt,
            OffsetDateTime createdAt) {
        this(orderId, orderNumber, status,
                totalAmount, totalAmount - shippingFee, shippingFee,
                0L, 0L, totalAmount, totalAmount - shippingFee, null,
                fulfillmentType, items, paidAt, approvalDeadlineAt, createdAt);
    }

    public static AdminOrderResponse from(
            Order order, Fulfillment fulfillment, List<OrderItem> orderItems) {
        return new AdminOrderResponse(
                order.getId(),
                "ORD-%08d".formatted(order.getId()),
                order.getStatus(),
                order.getTotalAmount(),
                order.getProductAmount(),
                order.getShippingFee(),
                order.getCouponDiscountAmount(),
                order.getRewardUsedAmount(),
                order.getPgPaidAmount(),
                order.getRewardEarnBase(),
                order.getIssuedCouponId(),
                fulfillment == null ? null : fulfillment.getType(),
                orderItems.stream().map(OrderItemView::from).toList(),
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                order.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
