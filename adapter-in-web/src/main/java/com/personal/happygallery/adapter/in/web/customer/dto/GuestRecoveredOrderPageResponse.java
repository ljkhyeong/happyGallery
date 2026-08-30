package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveredOrder;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GuestRecoveredOrderPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<GuestRecordRecoveryResponse.OrderSummary> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static GuestRecoveredOrderPageResponse from(CursorPage<RecoveredOrder> page) {
        return new GuestRecoveredOrderPageResponse(
                page.content().stream()
                        .map(GuestRecordRecoveryResponse.OrderSummary::from)
                        .toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
