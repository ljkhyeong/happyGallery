package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderOptionSnapshot;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.ProductOptionType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record AdminOrderView(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalAmount,
        long shippingFee,
        FulfillmentType fulfillmentType,
        List<OrderItemView> items,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        OffsetDateTime createdAt
) {

    public AdminOrderView {
        items = List.copyOf(items);
    }

    public record OrderItemView(
            Long productId,
            String productName,
            ProductType productType,
            int qty,
            long unitPrice,
            Long productVariantId,
            long basePrice,
            long variantPriceAdjustment,
            long textOptionPriceAdjustment,
            List<OrderOptionView> options,
            String specification,
            String careInstructions,
            Integer productionLeadDays
    ) {
        private static OrderItemView from(OrderItem item) {
            return new OrderItemView(
                    item.getProductId(),
                    item.getProductName(),
                    item.getProductType(),
                    item.getQty(),
                    item.getUnitPrice(),
                    item.getProductVariantId(),
                    item.getBasePrice(),
                    item.getVariantPriceAdjustment(),
                    item.getTextOptionPriceAdjustment(),
                    item.getOptionSnapshots().stream().map(OrderOptionView::from).toList(),
                    item.getSpecification(),
                    item.getCareInstructions(),
                    item.getProductionLeadDays());
        }
    }

    public record OrderOptionView(
            ProductOptionType type,
            String groupName,
            String value,
            long priceAdjustment
    ) {
        private static OrderOptionView from(OrderOptionSnapshot option) {
            return new OrderOptionView(
                    option.getType(), option.getGroupName(),
                    option.getValue(), option.getPriceAdjustment());
        }
    }

    public static AdminOrderView from(
            Order order, Fulfillment fulfillment, List<OrderItem> orderItems) {
        return new AdminOrderView(
                order.getId(),
                order.orderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingFee(),
                fulfillment == null ? null : fulfillment.getType(),
                orderItems.stream().map(OrderItemView::from).toList(),
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                order.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
