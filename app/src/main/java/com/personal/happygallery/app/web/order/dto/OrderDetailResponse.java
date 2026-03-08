package com.personal.happygallery.app.web.order.dto;

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
        LocalDateTime paidAt,
        LocalDateTime approvalDeadlineAt,
        List<ItemDto> items,
        FulfillmentDto fulfillment
) {
    public record ItemDto(Long productId, int qty, long unitPrice) {
        public static ItemDto from(OrderItem item) {
            return new ItemDto(item.getProductId(), item.getQty(), item.getUnitPrice());
        }
    }

    public record FulfillmentDto(String type, String status, LocalDate expectedShipDate,
                                 LocalDateTime pickupDeadlineAt) {
        public static FulfillmentDto from(Fulfillment f) {
            return new FulfillmentDto(
                    f.getType().name(),
                    f.getStatus().name(),
                    f.getExpectedShipDate(),
                    f.getPickupDeadlineAt()
            );
        }
    }

    public static OrderDetailResponse from(Order order, List<OrderItem> items, Fulfillment fulfillment) {
        return new OrderDetailResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getPaidAt(),
                order.getApprovalDeadlineAt(),
                items.stream().map(ItemDto::from).toList(),
                fulfillment != null ? FulfillmentDto.from(fulfillment) : null
        );
    }
}
