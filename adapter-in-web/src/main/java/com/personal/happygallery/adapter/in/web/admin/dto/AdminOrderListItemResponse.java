package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.AdminOrderResponse;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminOrderListItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String orderNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long totalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long shippingFee,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        FulfillmentType fulfillmentType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<AdminOrderItemResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        LocalDateTime paidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        LocalDateTime approvalDeadlineAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime createdAt
) {

    public AdminOrderListItemResponse {
        items = List.copyOf(items);
    }

    public static AdminOrderListItemResponse from(AdminOrderResponse order) {
        return new AdminOrderListItemResponse(
                order.orderId(),
                order.orderNumber(),
                order.status(),
                order.totalAmount(),
                order.shippingFee(),
                order.fulfillmentType(),
                order.items().stream().map(AdminOrderItemResponse::from).toList(),
                order.paidAt(),
                order.approvalDeadlineAt(),
                order.createdAt());
    }

    public record AdminOrderItemResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long productId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String productName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            ProductType productType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            int qty,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long unitPrice,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String specification,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String careInstructions,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            Integer productionLeadDays
    ) {
        private static AdminOrderItemResponse from(AdminOrderResponse.OrderItemView item) {
            return new AdminOrderItemResponse(
                    item.productId(),
                    item.productName(),
                    item.productType(),
                    item.qty(),
                    item.unitPrice(),
                    item.specification(),
                    item.careInstructions(),
                    item.productionLeadDays());
        }
    }
}
