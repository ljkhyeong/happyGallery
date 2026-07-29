package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.Refund;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FailedRefundPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<FailedRefundResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {

    public static FailedRefundPageResponse from(CursorPage<Refund> page) {
        return new FailedRefundPageResponse(
                page.content().stream().map(FailedRefundResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
