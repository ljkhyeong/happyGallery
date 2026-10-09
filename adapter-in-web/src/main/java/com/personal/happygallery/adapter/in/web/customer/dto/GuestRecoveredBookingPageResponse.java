package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestBookingSummary;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record GuestRecoveredBookingPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<BookingSummary> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static GuestRecoveredBookingPageResponse from(CursorPage<GuestBookingSummary> page) {
        return new GuestRecoveredBookingPageResponse(
                page.content().stream()
                        .map(BookingSummary::from)
                        .toList(),
                page.nextCursor(),
                page.hasMore());
    }

    @Schema(name = "GuestRecordRecoveryBookingSummary")
    public record BookingSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt
    ) {
        public static BookingSummary from(GuestBookingSummary booking) {
            return new BookingSummary(
                    booking.bookingId(), booking.status(), booking.className(),
                    booking.startAt(), booking.endAt());
        }
    }
}
