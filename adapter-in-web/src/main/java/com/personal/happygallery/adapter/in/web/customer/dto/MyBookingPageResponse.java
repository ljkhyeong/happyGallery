package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyBookingPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<MyBookingSummary> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static MyBookingPageResponse from(CursorPage<Booking> page) {
        return new MyBookingPageResponse(
                MyBookingSummary.fromAll(page.content()),
                page.nextCursor(),
                page.hasMore());
    }
}
