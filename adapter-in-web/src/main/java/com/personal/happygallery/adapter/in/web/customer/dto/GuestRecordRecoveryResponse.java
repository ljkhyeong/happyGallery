package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveredBooking;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveredOrder;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveryResult;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record GuestRecordRecoveryResponse(
        String accessToken,
        OffsetDateTime expiresAt,
        List<OrderSummary> orders,
        List<BookingSummary> bookings
) {
    public static GuestRecordRecoveryResponse from(RecoveryResult result) {
        return new GuestRecordRecoveryResponse(
                result.accessToken(),
                result.expiresAt().atOffset(ZoneOffset.UTC),
                result.orders().stream().map(OrderSummary::from).toList(),
                result.bookings().stream().map(BookingSummary::from).toList());
    }

    public record OrderSummary(Long orderId, String status, long totalAmount, OffsetDateTime createdAt) {
        private static OrderSummary from(RecoveredOrder order) {
            return new OrderSummary(order.orderId(), order.status(), order.totalAmount(), order.createdAt());
        }
    }

    public record BookingSummary(Long bookingId,
                                 String status,
                                 String className,
                                 LocalDateTime startAt,
                                 LocalDateTime endAt) {
        private static BookingSummary from(RecoveredBooking booking) {
            return new BookingSummary(
                    booking.bookingId(), booking.status(), booking.className(),
                    booking.startAt(), booking.endAt());
        }
    }
}
