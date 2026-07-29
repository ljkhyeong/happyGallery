package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record GuestClaimPreviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean phoneVerified,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<OrderSummary> orders,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BookingSummary> bookings
) {
    public static GuestClaimPreviewResponse from(GuestClaimUseCase.ClaimPreview preview) {
        return new GuestClaimPreviewResponse(
                preview.phoneVerified(),
                preview.orders().stream().map(OrderSummary::from).toList(),
                preview.bookings().stream().map(BookingSummary::from).toList());
    }

    public record OrderSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
    ) {
        private static OrderSummary from(GuestClaimUseCase.ClaimOrderSummary summary) {
            return new OrderSummary(
                    summary.orderId(),
                    summary.status(),
                    summary.totalAmount(),
                    summary.createdAt());
        }
    }

    public record BookingSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt
    ) {
        private static BookingSummary from(GuestClaimUseCase.ClaimBookingSummary summary) {
            return new BookingSummary(
                    summary.bookingId(),
                    summary.status(),
                    summary.className(),
                    summary.startAt(),
                    summary.endAt());
        }
    }
}
