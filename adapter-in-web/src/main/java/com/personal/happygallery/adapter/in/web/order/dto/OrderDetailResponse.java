package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String orderNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long totalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long productAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long shippingFee,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long couponDiscountAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long rewardUsedAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long pgPaidAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long rewardEarnBase,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long issuedCouponId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        LocalDateTime paidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        LocalDateTime approvalDeadlineAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ItemDto> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        FulfillmentDto fulfillment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        RefundProgressResponse refund
) {
    public record ItemDto(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long orderItemId,
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
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long grossAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long couponDiscountAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long rewardUsedAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long netPaidAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String specification,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String careInstructions,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            Integer productionLeadDays
    ) {
        public static ItemDto from(OrderItem item) {
            return new ItemDto(
                    item.getId(),
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

    public record FulfillmentDto(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            FulfillmentType type,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            LocalDate expectedShipDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            LocalDateTime pickupDeadlineAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String carrier,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String trackingNumber,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            ShippingAddressDto shippingAddress) {
        public static FulfillmentDto from(Fulfillment f, ShippingAddress shippingAddress) {
            return new FulfillmentDto(
                    f.getType(),
                    f.getExpectedShipDate(),
                    f.getPickupDeadlineAt(),
                    f.getCarrier(),
                    f.getTrackingNumber(),
                    shippingAddress != null ? ShippingAddressDto.from(shippingAddress) : null
            );
        }
    }

    public record ShippingAddressDto(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String recipientName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String phone,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String postalCode,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String addressLine1,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String addressLine2
    ) {
        private static ShippingAddressDto from(ShippingAddress address) {
            return new ShippingAddressDto(
                    address.recipientName(),
                    address.phone(),
                    address.postalCode(),
                    address.addressLine1(),
                    address.addressLine2());
        }
    }

    public static OrderDetailResponse from(OrderQueryUseCase.OrderDetail detail) {
        Order order = detail.order();
        return new OrderDetailResponse(
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
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                detail.items().stream().map(ItemDto::from).toList(),
                detail.fulfillment() != null
                        ? FulfillmentDto.from(detail.fulfillment(), detail.shippingAddress())
                        : null,
                detail.refund() != null ? RefundProgressResponse.from(detail.refund()) : null
        );
    }
}
