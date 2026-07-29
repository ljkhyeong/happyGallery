package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminOrderSearchPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Order> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPages
) {

    public static AdminOrderSearchPageResponse from(OffsetPage<AdminOrderSearchRow> page) {
        return new AdminOrderSearchPageResponse(
                page.content().stream().map(Order::from).toList(),
                page.page(),
                page.size(),
                page.totalCount(),
                page.totalPages());
    }

    @Schema(name = "AdminOrderSearchResult")
    public record Order(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orderNumber,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String buyerName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String buyerPhone,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime paidAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            LocalDateTime approvalDeadlineAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
    ) {

        private static Order from(AdminOrderSearchRow row) {
            return new Order(
                    row.orderId(),
                    row.orderNumber(),
                    OrderStatus.valueOf(row.status()),
                    row.totalAmount(),
                    row.buyerName(),
                    row.buyerPhone(),
                    row.paidAt(),
                    row.approvalDeadlineAt(),
                    row.createdAt());
        }
    }
}
