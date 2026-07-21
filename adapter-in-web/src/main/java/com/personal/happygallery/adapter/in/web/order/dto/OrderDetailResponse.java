package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        String status,
        long totalAmount,
        long shippingFee,
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        List<ItemDto> items,
        FulfillmentDto fulfillment,
        RefundProgressResponse refund
) {
    public record ItemDto(Long productId, String productName, int qty, long unitPrice) {
        public static ItemDto from(OrderItem item) {
            return new ItemDto(
                    item.getProductId(), item.getProductName(), item.getQty(), item.getUnitPrice());
        }
    }

    public record FulfillmentDto(String type, LocalDate expectedShipDate,
                                 LocalDateTime pickupDeadlineAt,
                                 String carrier,
                                 String trackingNumber) {
        public static FulfillmentDto from(Fulfillment f) {
            return new FulfillmentDto(
                    f.getType().name(),
                    f.getExpectedShipDate(),
                    f.getPickupDeadlineAt(),
                    f.getCarrier(),
                    f.getTrackingNumber()
            );
        }
    }

    public static OrderDetailResponse from(OrderQueryUseCase.OrderDetail detail) {
        Order order = detail.order();
        return new OrderDetailResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getShippingFee(),
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                detail.items().stream().map(ItemDto::from).toList(),
                detail.fulfillment() != null ? FulfillmentDto.from(detail.fulfillment()) : null,
                detail.refund() != null ? RefundProgressResponse.from(detail.refund()) : null
        );
    }
}
