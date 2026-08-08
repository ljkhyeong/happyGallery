package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveredBooking;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GuestRecoveredBookingPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<GuestRecordRecoveryResponse.BookingSummary> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static GuestRecoveredBookingPageResponse from(CursorPage<RecoveredBooking> page) {
        return new GuestRecoveredBookingPageResponse(
                page.content().stream()
                        .map(GuestRecordRecoveryResponse.BookingSummary::from)
                        .toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
