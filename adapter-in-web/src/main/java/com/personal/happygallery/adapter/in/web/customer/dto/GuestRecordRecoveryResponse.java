package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveredBooking;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveredOrder;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record GuestRecordRecoveryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<OrderSummary> orders,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BookingSummary> bookings
) {
    public static GuestRecordRecoveryResponse from(RecoveryResult result) {
        return new GuestRecordRecoveryResponse(
                result.accessToken(),
                result.expiresAt().atOffset(ZoneOffset.UTC),
                result.orders().stream().map(OrderSummary::from).toList(),
                result.bookings().stream().map(BookingSummary::from).toList());
    }

    @Schema(name = "GuestRecordRecoveryOrderSummary")
    public record OrderSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {
                            "PAID_APPROVAL_PENDING", "APPROVED_FULFILLMENT_PENDING",
                            "REJECTED", "CUSTOMER_CANCELED", "AUTO_REFUND_TIMEOUT",
                            "IN_PRODUCTION", "DELAY_CONSENT_PENDING", "DELAY_ACCEPTED",
                            "DELAY_REJECTED_CANCELED", "SHIPPING_PREPARING", "SHIPPED",
                            "DELIVERED", "PICKUP_READY", "PICKED_UP", "PICKUP_EXPIRED",
                            "PICKUP_FORFEITED", "COMPLETED"
                    })
            String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
    ) {
        private static OrderSummary from(RecoveredOrder order) {
            return new OrderSummary(order.orderId(), order.status(), order.totalAmount(), order.createdAt());
        }
    }

    @Schema(name = "GuestRecordRecoveryBookingSummary")
    public record BookingSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {"BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"})
            String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt
    ) {
        private static BookingSummary from(RecoveredBooking booking) {
            return new BookingSummary(
                    booking.bookingId(), booking.status(), booking.className(),
                    booking.startAt(), booking.endAt());
        }
    }
}
